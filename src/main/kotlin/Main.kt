@file:JvmName("Main")

package pdfbox_sample

import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.contentstream.operator.OperatorName
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSNumber
import org.apache.pdfbox.cos.COSString
import org.apache.pdfbox.pdfparser.PDFStreamParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList
import org.apache.pdfbox.util.Matrix
import java.io.ByteArrayInputStream
import java.io.File

fun main() {
  val file = File("sample.pdf")

  PDDocument.load(file).use { document ->
    PDDocument().use { document2 ->
      replaceDocument(document, document2)
      document2.save("sample_dest.pdf")
    }
  }
}

fun replaceDocument(doc: PDDocument, doc2: PDDocument) {

  // IPAex font
  // https://moji.or.jp/ipafont/ipaex00401/
  // https://moji.or.jp/wp-content/ipafont/IPAexfont/ipaexg00401.zip

  val font = PDType0Font.load(doc, File("ipaexg.ttf"))


  for (page in doc.documentCatalog.pages) {
    val page2 = PDPage()
    doc2.addPage(page2)

    PDPageContentStream(doc2, page2).use { page2Stream ->
      replacePage(page, page2Stream, font)
    }
  }
}

fun replacePage(page: PDPage, page2Stream: PDPageContentStream, fontDefault: PDFont) {
  var font: PDFont = fontDefault

  val parser = PDFStreamParser(page)
  parser.parse()

  val tokens = parser.tokens
  for (tokenIndex in tokens.indices) {
    val token = tokens[tokenIndex]

    if (token !is Operator) {
      continue
    }

    when (token.name) {
      //テキスト
      OperatorName.BEGIN_TEXT -> {
        page2Stream.beginText()
      }
      OperatorName.MOVE_TEXT -> {
        val x = (tokens[tokenIndex - 2] as COSNumber).floatValue()
        val y = (tokens[tokenIndex - 1] as COSNumber).floatValue()
        page2Stream.newLineAtOffset(x, y)
      }
      OperatorName.SET_MATRIX -> {
        val ary = COSArray()
        ary.addAll((6 downTo 1).map {
          tokens[tokenIndex - it] as COSNumber
        })
        page2Stream.setTextMatrix(Matrix(ary))
      }
      OperatorName.SET_FONT_AND_SIZE -> {
        val fontNameObj = tokens[tokenIndex - 2] as COSName
        val fontSizeObj = tokens[tokenIndex - 1] as COSNumber
        font = page.resources.getFont(fontNameObj)
        val fontSize = fontSizeObj.floatValue()
        page2Stream.setFont(fontDefault, fontSize)
      }
      OperatorName.SHOW_TEXT -> {
        val previous = tokens[tokenIndex - 1] as COSString
        val buffer = parsePdfString(font, previous)
        val buffer2 = replacePdfText(buffer)
        page2Stream.showText(buffer2)
      }
      OperatorName.SHOW_TEXT_ADJUSTED -> {
        val previous = tokens[tokenIndex - 1] as COSArray
        val buffer = previous.filterIsInstance<COSString>().joinToString("") { obj ->
          parsePdfString(font, obj)
        }

        val buffer2 = replacePdfText(buffer)
        page2Stream.showText(buffer2)
      }
      OperatorName.END_TEXT -> {
        page2Stream.endText()
      }
      //罫線
      OperatorName.MOVE_TO -> {
        val x = (tokens[tokenIndex - 2] as COSNumber).floatValue()
        val y = (tokens[tokenIndex - 1] as COSNumber).floatValue()
        page2Stream.moveTo(x, y)
      }
      OperatorName.LINE_TO -> {
        val x = (tokens[tokenIndex - 2] as COSNumber).floatValue()
        val y = (tokens[tokenIndex - 1] as COSNumber).floatValue()
        page2Stream.lineTo(x, y)
      }
      OperatorName.STROKE_PATH -> {
        page2Stream.stroke()
      }
    }
  }
}

fun parsePdfString(font: PDFont, obj: COSString): String {
  val buffer = StringBuffer()

  val input = ByteArrayInputStream(obj.bytes)
  while (input.available() > 0) {
    val code = font.readCode(input)
    val ch = font.toUnicode(code, GlyphList.getAdobeGlyphList())
    buffer.append(ch)
  }

  return buffer.toString()
}

fun replacePdfText(text: String): String {
  return text.replace("%NAME%", "ほげほげ")
    .replace("%ADDRESS%", "東京都千代田区千代田1-1")
    .replace("%TEL%", "012-3456-7890")
}
