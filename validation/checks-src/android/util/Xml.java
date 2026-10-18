package android.util;

import java.io.Reader;
import java.lang.reflect.Proxy;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.xmlpull.v1.XmlPullParser;

/** A real JDK XML tokenizer behind the framework interface; production owns all vector validation. */
public final class Xml {
    public static XmlPullParser newPullParser(){
        final XMLStreamReader[] reader={null};final int[] event={XmlPullParser.START_DOCUMENT};
        return (XmlPullParser)Proxy.newProxyInstance(Xml.class.getClassLoader(),new Class<?>[]{XmlPullParser.class},(proxy,method,args)->{
            switch(method.getName()){
                case "setFeature":return null;
                case "setInput":{
                    XMLInputFactory factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
                    factory.setXMLResolver((publicId,systemId,base,namespace)->{throw new javax.xml.stream.XMLStreamException("External input forbidden");});reader[0]=factory.createXMLStreamReader((Reader)args[0]);event[0]=XmlPullParser.START_DOCUMENT;return null;
                }
                case "getEventType":return event[0];
                case "next":case "nextToken":{
                    if(!reader[0].hasNext()){event[0]=XmlPullParser.END_DOCUMENT;return event[0];}int type=reader[0].next();
                    switch(type){case XMLStreamConstants.START_ELEMENT:event[0]=XmlPullParser.START_TAG;break;case XMLStreamConstants.END_ELEMENT:event[0]=XmlPullParser.END_TAG;break;
                        case XMLStreamConstants.END_DOCUMENT:event[0]=XmlPullParser.END_DOCUMENT;break;case XMLStreamConstants.DTD:event[0]=XmlPullParser.DOCDECL;break;
                        case XMLStreamConstants.ENTITY_REFERENCE:event[0]=XmlPullParser.ENTITY_REF;break;case XMLStreamConstants.PROCESSING_INSTRUCTION:event[0]=XmlPullParser.PROCESSING_INSTRUCTION;break;
                        case XMLStreamConstants.COMMENT:event[0]=XmlPullParser.COMMENT;break;default:event[0]=XmlPullParser.TEXT;}
                    return event[0];
                }
                case "getName":return reader[0].getLocalName();
                case "getNamespace":return reader[0].getNamespaceURI()==null?"":reader[0].getNamespaceURI();
                case "getText":return reader[0].hasText()?reader[0].getText():"";
                case "getAttributeCount":return reader[0].getAttributeCount();
                case "getAttributeName":return reader[0].getAttributeLocalName((Integer)args[0]);
                case "getAttributeNamespace":{String namespace=reader[0].getAttributeNamespace((Integer)args[0]);return namespace==null?"":namespace;}
                case "getAttributeValue":return reader[0].getAttributeValue((Integer)args[0]);
                case "toString":return "JDK XML event fixture";case "hashCode":return System.identityHashCode(proxy);case "equals":return proxy==args[0];
                default:throw new UnsupportedOperationException(method.getName());
            }
        });
    }
}
