package com.example.kostowski.legolist

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import android.R.attr.entries
import android.content.Context
import android.databinding.ObservableArrayList
import android.os.Environment
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class XMLParser(c: Context) {
    private val ns: String?=null
    private var db: DatabaseHelper = DatabaseHelper(c)


    fun parseFromLink(url: String): ObservableArrayList<Brick>{
        var effectList: ObservableArrayList<Brick> = ObservableArrayList()
        val parser: XmlPullParser = Xml.newPullParser()
        val inputS: InputStream = URL(url).openStream()

        try{
            db.createDataBase()
        }catch (e: Exception){
            e.printStackTrace()
        }
        try {
            db.openDataBase()
        }catch (e: Exception){
            e.printStackTrace()
        }

        try{
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputS, null)
            parser.nextTag()

            parser.require(XmlPullParser.START_TAG, ns, "INVENTORY")
            while (parser.next() !== XmlPullParser.END_TAG) {
                if (parser.eventType !== XmlPullParser.START_TAG) {
                    continue
                }
                val name = parser.name
                // Starts by looking for the entry tag
                if (name == "ITEM") {
                    effectList.add(readItem(parser))
                } else {
                    skip(parser)
                }
            }

        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            inputS.close()
        }

        return effectList
    }

    fun readItem(parser: XmlPullParser): Brick{
        parser.require(XmlPullParser.START_TAG, ns, "ITEM")

        var itemType: String = ""
        var itemId: String = ""
        var itemColor: Int = 0
        var itemNum: Int = 0

        while (parser.next() !== XmlPullParser.END_TAG) {
            if (parser.eventType !== XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name

            when(name){
                "ITEMTYPE" -> {
                    parser.require(XmlPullParser.START_TAG, ns, "ITEMTYPE")
                    if(parser.next()==XmlPullParser.TEXT){
                        itemType=parser.text
                        parser.nextTag()
                    }
                    parser.require(XmlPullParser.END_TAG, ns, "ITEMTYPE")
                }
                "ITEMID" -> {
                    parser.require(XmlPullParser.START_TAG, ns, "ITEMID")
                    if(parser.next()==XmlPullParser.TEXT){
                        itemId=parser.text
                        parser.nextTag()
                    }
                    parser.require(XmlPullParser.END_TAG, ns, "ITEMID")
                }
                "QTY" -> {
                    parser.require(XmlPullParser.START_TAG, ns, "QTY")
                    if(parser.next()==XmlPullParser.TEXT){
                        itemNum=parser.text.toInt()
                        parser.nextTag()
                    }
                    parser.require(XmlPullParser.END_TAG, ns, "QTY")
                }
                "COLOR" -> {
                    parser.require(XmlPullParser.START_TAG, ns, "COLOR")
                    if(parser.next()==XmlPullParser.TEXT){
                        itemColor=parser.text.toInt()
                        parser.nextTag()
                    }
                    parser.require(XmlPullParser.END_TAG, ns, "COLOR")
                }
                else -> skip(parser)
            }
        }

        var colorName = db.fetchColorName(itemColor)
        var name = db.fetchName(itemId)
        var typeName = db.fetchTypeName(itemType)

        return Brick(name, typeName, itemColor, itemId, itemType, 0, itemNum, colorName, db.fetchCode(db.getBrickId(itemId), itemColor))
    }

    fun skip(parser: XmlPullParser) {
        var depth: Int = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }

        }
    }

    fun exportToXMLFile(project: Project, fileName: String){
        val docBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()

        val rootElement: Element = doc.createElement("INVENTORY")


        for(block in project.listOfNeededBricks){
            if(!block.done){
                var itemNode: Element = doc.createElement("ITEM")
                var itemType: Element = doc.createElement("ITEMTYPE")
                var itemId: Element = doc.createElement("ITEMID")
                var itemColor: Element = doc.createElement("COLOR")
                var itemQty: Element = doc.createElement("QTYFILLED")


                itemType.appendChild(doc.createTextNode(block.brickTypeCode))
                itemNode.appendChild(itemType)

                itemId.appendChild(doc.createTextNode(block.blockIdCode))
                itemNode.appendChild(itemId)

                itemColor.appendChild(doc.createTextNode(block.colorCode.toString()))
                itemNode.appendChild(itemColor)

                itemQty.appendChild(doc.createTextNode((block.maxNumber-block.collectedNumber).toString()))
                itemNode.appendChild(itemQty)

                rootElement.appendChild(itemNode)
            }
        }

        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()

        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")



        val file = File(Environment.getExternalStorageDirectory().absolutePath, fileName)

        transformer.transform(DOMSource(doc), StreamResult(file))
    }
}