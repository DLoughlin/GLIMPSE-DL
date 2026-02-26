/*
* LEGAL NOTICE
* This computer software was prepared by US EPA.
* THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
* LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
* sentence must appear on any copies of this computer software.
* 
* EXPORT CONTROL
* User agrees that the Software will not be shipped, transferred or
* exported into any country or used in any manner prohibited by the
* United States Export Administration Act or any other applicable
* export laws, restrictions or regulations (collectively the "Export Laws").
* Export of the Software may require some form of license or other
* authority from the U.S. Government, and failure to obtain such
* export control license may result in criminal liability under
* U.S. laws. In addition, if the Software is identified as export controlled
* items under the Export Laws, User represents and warrants that User
* is not a citizen, or otherwise located within, an embargoed nation
* (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
*
* SUPPORT
* GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. 
* The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
* Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
* Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
* Laboratory contract. 
* 
*/



package glimpseBuilder;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// Added for optional fallback pretty printer
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class XMLModifier {

	Document xmlDocument = null;

	public static Document openXmlDocument(String filename) {
		File xmlFile = new File(filename);
		return openXmlDocument(xmlFile);
	}
	
	public static Document openXmlDocument(File xmlFile) {
		//File xmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;

		Document doc = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			System.out.println("Error opening xml document: " + e);
		}

		return doc;
	}

	public static void writeXmlDocument(Document doc, String filename) {
		writeXmlDocument(doc, filename, 4);
	}

	/**
	 * Writes an XML document to a file with pretty-print indentation.
	 * Note: indentation behavior can vary across JRE transformer implementations.
	 */
	public static void writeXmlDocument(Document doc, String filename, int indentSpaces) {
		try {
			// IMPORTANT:
			// If the document was parsed from an already-indented XML, the DOM typically contains
			// whitespace-only text nodes ("\n  "), and most serializers/transformers will preserve
			// those nodes instead of re-indenting. Remove them first so pretty-print can take effect.
			stripWhitespaceOnlyTextNodes(doc);
			doc.getDocumentElement().normalize();

			// First attempt: JAXP Transformer pretty-print
			boolean wrote = tryWriteWithTransformer(doc, new File(filename), indentSpaces);
			if (!wrote) {
				// Fallback: DOM Load/Save serializer pretty-print (often succeeds when Transformer ignores indentation)
				tryWriteWithLSSerializer(doc, new File(filename), indentSpaces);
			}

			System.out.println("XML file updated successfully");
			System.out.println("---");
		} catch (Exception e) {
			System.out.println("Error updating XML file: " + e);
		}
	}

	/**
	 * Removes whitespace-only text nodes from the DOM. These nodes are commonly introduced
	 * when parsing pretty-printed XML and can prevent re-indentation on write.
	 */
	private static void stripWhitespaceOnlyTextNodes(Node node) {
		if (node == null) return;

		Node child = node.getFirstChild();
		while (child != null) {
			Node next = child.getNextSibling();
			switch (child.getNodeType()) {
				case Node.TEXT_NODE:
					if (child.getTextContent() != null && child.getTextContent().trim().isEmpty()) {
						node.removeChild(child);
					}
					break;
				case Node.ELEMENT_NODE:
				case Node.DOCUMENT_NODE:
					stripWhitespaceOnlyTextNodes(child);
					break;
				default:
					// Keep comments, CDATA, processing instructions, etc.
					break;
			}
			child = next;
		}
	}

	private static boolean tryWriteWithTransformer(Document doc, File outFile, int indentSpaces)
			throws TransformerException {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// Different transformer implementations look for different knobs.
			String indent = String.valueOf(Math.max(0, indentSpaces));
			try {
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent);
			} catch (IllegalArgumentException ignored) {
				// Ignore if unsupported
			}
			try {
				transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", indent);
			} catch (IllegalArgumentException ignored) {
				// Ignore if unsupported
			}

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outFile);
			transformer.transform(source, result);
			return true;
		} catch (TransformerException e) {
			// If the failure is due to an I/O error (e.g. permission denied, disk full),
			// it is not recoverable — re-throw to prevent a silent fallback overwriting the error.
			if (e.getCause() instanceof IOException) {
				throw e;
			}
			// Otherwise the transformer may simply not support pretty-print; log and fall back.
			System.out.println("Transformer write attempt failed (will try fallback): " + e.getMessage());
			return false;
		}
	}

	private static void tryWriteWithLSSerializer(Document doc, File outFile, int indentSpaces) throws Exception {
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
		LSSerializer serializer = impl.createLSSerializer();

		// Pretty print if supported
		if (serializer.getDomConfig().canSetParameter("format-pretty-print", Boolean.TRUE)) {
			serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
		}
		// Prevent errors on some parsers when encountering CDATA sections
		if (serializer.getDomConfig().canSetParameter("cdata-sections", Boolean.TRUE)) {
			serializer.getDomConfig().setParameter("cdata-sections", Boolean.TRUE);
		}

		LSOutput output = impl.createLSOutput();
		output.setSystemId(outFile.toURI().toString());
		output.setEncoding("UTF-8");

		// Note: LSSerializer does not allow controlling indent width portably.
		serializer.write(doc, output);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filePath = "C:\\projects\\GCAM-GUI\\test\\configuration_GCAM_4p2_Original.xml";
		File xmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			// add new element
			addElement(doc, "String", "Value", "newElement", "barbasol shaving cream");

			// update Element value
			updateElementValue(doc, "String", "Value", "newElement", "barbasol shaving creams");

			// delete element
			deleteElement(doc, "String", "Value", "MAGICC-input-dir");

			// write the updated document to file or console
			doc.getDocumentElement().normalize();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(
					"c:\\projects\\GCAM-GUI\\test\\configuration_GCAM_4p2_Updated.xml"));
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // Added for pretty-printing
			transformer.transform(source, result);
			System.out.println("XML file updated successfully"); System.out.println("---");

		} catch (SAXException | ParserConfigurationException | IOException | TransformerException e1) {
			e1.printStackTrace();
		}
	}

	public static void addElement(Document doc, String topNodeStr, String subNodeStr, String attributeNameStr,
			String attributeValStr) {
		NodeList nodeList = doc.getElementsByTagName(topNodeStr);
		Element node = null;
		// loop for each employee
		for (int i = 0; i < nodeList.getLength(); i++) {
			node = (Element) nodeList.item(i);
			Element newElement = doc.createElement(subNodeStr);
			newElement.setAttribute("name", attributeNameStr);
			newElement.appendChild(doc.createTextNode(attributeValStr));
			node.appendChild(newElement);
		}
	}

	public static void deleteElement(Document doc, String topNodeStr, String subNodeStr, String attributeValStr) {
		NodeList nodeList = doc.getElementsByTagName(topNodeStr);
		NodeList subNodeList;
		Element node = null;
		Element subNode = null;
		// loop for each employee
		for (int i = 0; i < nodeList.getLength(); i++) {
			node = (Element) nodeList.item(i);
			subNodeList = node.getElementsByTagName(subNodeStr);
			for (int j = 0; j < subNodeList.getLength(); j++) {
				subNode = (Element) subNodeList.item(j);
				String attributeName = subNode.getAttribute("name");
				// System.out.println(".... subnodename " + subNodeName +
				// " attributename " + attributeName);
				if (attributeName.equals(attributeValStr)) {
					// System.out.println("   ....match");
					node.removeChild(subNode);
					break;
				}
			}
		}

	}

	public static void updateElementValue(Document doc, String topNodeStr, String subNodeStr, String attributeNameStr,
			String attributeValStr) {
		NodeList nodeList = doc.getElementsByTagName(topNodeStr);
		NodeList subNodeList;
		Element node = null;
		Element subNode = null;
		// loop for each employee
		for (int i = 0; i < nodeList.getLength(); i++) {

			node = (Element) nodeList.item(i);
			// System.out.println("i " + i + " " + node.getNodeName() + " ");
			subNodeList = node.getElementsByTagName(subNodeStr);

			for (int j = 0; j < subNodeList.getLength(); j++) {
				subNode = (Element) subNodeList.item(j);
				String attributeName = subNode.getAttribute("name");
				String attributeValue = subNode.getTextContent();

				// System.out.println("  --> j " + j + " " + topNodeName + " " +
				// subNodeName + " " + attributeName + " "
				// + attributeValue);
				if (attributeName.equals(attributeNameStr)) {
					attributeValue = attributeValue.toUpperCase();
					subNode.setTextContent(attributeValStr);
				}
			}

		}

	}

	public static void updateAttributeValue(Document doc, String topNodeStr, String subNodeStr, String nameStr, String attributeNameStr,
			String attributeValStr) {
		NodeList nodeList = doc.getElementsByTagName(topNodeStr);
		NodeList subNodeList;
		Element node = null;
		Element subNode = null;
		// loop for each employee
		
		System.out.println("tried to set :"+attributeNameStr+": to :"+attributeValStr+":-> top:"+topNodeStr+" sub:"+subNodeStr+" ValueName:"+nameStr+": attr:"+attributeNameStr); 
		
		for (int i = 0; i < nodeList.getLength(); i++) {

			node = (Element) nodeList.item(i);
			// System.out.println("i " + i + " " + node.getNodeName() + " ");
			subNodeList = node.getElementsByTagName(subNodeStr);

			for (int j = 0; j < subNodeList.getLength(); j++) {
				subNode = (Element) subNodeList.item(j);
				String topNodeName = node.getTagName();
				String subNodeName = subNode.getTagName();
				String valueName = subNode.getAttribute("name");
				if (topNodeName.equals(topNodeStr)) {
					if (subNodeName.equals(subNodeStr)){
						if (valueName.equals(nameStr)) {
							subNode.setAttribute(attributeNameStr,attributeValStr);
						}
					}
				}

			}

		}

	}
	
	
}