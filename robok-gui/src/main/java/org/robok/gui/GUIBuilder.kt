package org.robok.gui

/*
 *  This file is part of Robok © 2024.
 *
 *  Robok is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Robok is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with Robok.  If not, see <https://www.gnu.org/licenses/>.
 */ 

import android.content.Context

import org.robok.gui.internal.DefaultValues
import org.robok.gui.internal.newLine
import org.robok.gui.internal.newLineBroken
import org.robok.gui.converter.AttributeConverter

import java.lang.reflect.InvocationTargetException

/*
* Class that generates XML from the received data.
*/

class GUIBuilder (
    private val context: Context,
    private val codeComments: Boolean = false,
    private val onGenerateCode: (String) -> Unit,
    val onError: (String) -> Unit
) {
    val xmlCodeList: MutableList<String> = mutableListOf()
    private var indentLevel = 0
    private val indent: String
        get() = "\t".repeat(indentLevel)
        
    val closingTagLayoutList: MutableList<String> = mutableListOf()
    var attributeConverter: AttributeConverter? = null
    
    init {
         rootView()
         attributeConverter = AttributeConverter()
    }
        
    private fun rootView() {
         if (codeComments) xmlCodeList.newLineBroken("<!-- Opening Root Layout -->")
         xmlCodeList.newLineBroken("<LinearLayout")
         indentLevel++
         xmlCodeList.newLineBroken(DefaultValues.XMLNS(indent))
         xmlCodeList.newLineBroken("${indent}${DefaultValues.LAYOUT_HEIGHT}")
         xmlCodeList.newLine("${indent}${DefaultValues.LAYOUT_WIDTH}")
         xmlCodeList.newLineBroken(">")
         indentLevel++
    }

    fun Column() {
         if (codeComments) xmlCodeList.newLineBroken("<!-- Opening Column Layout -->")
         xmlCodeList.newLineBroken("${indent}<LinearLayout")
         indentLevel++
         /*
         xmlCodeList.newLineBroken("${indent}${DefaultValues.LAYOUT_HEIGHT}")
         xmlCodeList.newLine("${indent}${DefaultValues.LAYOUT_WIDTH}")
         */
         xmlCodeList.newLineBroken(">")
         indentLevel++
         closingTagLayoutList.add("Column:</LinearLayout>")
    }
    
    fun Text() {
         if (codeComments) xmlCodeList.newLineBroken("<!-- Text Component -->")
         xmlCodeList.newLineBroken("${indent}<TextView")
         indentLevel++
         closingTagLayoutList.add("Text:/>")
    }
    
    fun Button() {
         if (codeComments) xmlCodeList.newLineBroken("<!-- Button  Component -->")
         xmlCodeList.newLineBroken("${indent}<Button")
         indentLevel++
         closingTagLayoutList.add("Button:/>")
    }
    
    fun newLog(log: String){
         if (codeComments) xmlCodeList.add(log)
    }
    
    fun closeBlock() {
         if (closingTagLayoutList.isNotEmpty()) {
             val tags = closingTagLayoutList.last().split(":")
  
             if (tags.size >= 2) {
                 val closingTagGui = tags[0]
                 val closingTagXml = tags[1]
                
                 if (codeComments) xmlCodeList.newLineBroken("<!-- Closing $closingTagGui Layout -->")
                 
                 if(closingTagXml.equals("/>")){
                     var previousAttribute: String = xmlCodeList.last()
                     if(previousAttribute.contains("\n")) previousAttribute = previousAttribute.replace("\n", "")
                    
                     xmlCodeList.removeAt(xmlCodeList.size - 1)
                     xmlCodeList.newLineBroken(previousAttribute + closingTagXml)
                 } else {
                    xmlCodeList.newLineBroken("${indent}$closingTagXml" + "\n")
                 }
                 indentLevel--
                 if (codeComments) xmlCodeList.newLineBroken("<!-- removing " + closingTagLayoutList.get((closingTagLayoutList.size - 1)))
                 closingTagLayoutList.removeAt(closingTagLayoutList.size - 1)
             } else {
                 onError("Error: invalid tag format  tag of closing.")
             }
         } else {
             onError("Error: No layout to close.")
         }
    }
    
    fun runMethod(methodName: String) {
         try {
             val method = this::class.java.getDeclaredMethod(methodName)
             method.invoke(this)
         } catch (e: InvocationTargetException) {
             onError(e.toString())
         }
    }
    
    fun runMethodWithParameters(methodName: String, vararg args: Any?) {
         try {
             val parameterTypes = args.map { it?.javaClass }.toTypedArray()
             val method = this::class.java.getDeclaredMethod(methodName, *parameterTypes)
             method.invoke(this, *args)
         } catch (e: NoSuchMethodException) {
             onError(e.toString())
         } catch (e: InvocationTargetException) {
             val originalException = e.cause
             onError(e.toString())
         } catch (e: IllegalAccessException) {
             onError(e.toString())
         }
    }  
      
    fun addAttribute(methodName: String, key: String, value: String){
         var containsCloseTag = false
         var attribute = ""
         
         if(xmlCodeList.get((xmlCodeList.size -1)).contains("/>")){
             containsCloseTag = true
             attribute = xmlCodeList.last().replace("/>","").replace("\n","")
             xmlCodeList.removeAt(xmlCodeList.size - 1)
             xmlCodeList.newLineBroken(attribute)
             indentLevel++
         }
         
         indentLevel++
         val attributeConverted = attributeConverter?.convert(key)
         xmlCodeList.newLineBroken(indent + attributeConverted + "=" + "\"$value\"")
         indentLevel--
            
         if(containsCloseTag){
             closingTagLayoutList.add("$methodName:/>")
             closeBlock()
         }
    }
    
    fun buildXML(): String {
         var codes: StringBuilder = StringBuilder()
        
         xmlCodeList.forEach {
             codes.append(it)
         }
        
         return codes.toString()
    }
    
    fun finish(){
         indentLevel--
         indentLevel--
         if (codeComments) xmlCodeList.newLineBroken("<!-- closing Root Layout -->")
         xmlCodeList.newLineBroken("</LinearLayout>")
         if (codeComments) xmlCodeList.add("\nEnd.")
         onGenerateCode(buildXML())
    }
}