package com.example.lectureHomework;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import soapclient.MNBArfolyamServiceSoap;
import soapclient.MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage;
import soapclient.MNBArfolyamServiceSoapImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@Controller
public class HomeController {
    @GetMapping("/")
    public String index(){
        return "index";
    }
    @GetMapping("/home")
    public String home(){return "home";}

    @GetMapping("/soap")
    public String soap(Model model) {
        model.addAttribute("param", new MessagePrice());
        return "soapform";
    }
    @PostMapping("/soap")
    public String soap2(@ModelAttribute MessagePrice messagePrice, Model model) throws
            MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage {

        MNBArfolyamServiceSoapImpl impl = new MNBArfolyamServiceSoapImpl();
        MNBArfolyamServiceSoap service = impl.getCustomBindingMNBArfolyamServiceSoap();

        String xml = service.getExchangeRates(
                messagePrice.getStartDate(),
                messagePrice.getEndDate(),
                messagePrice.getCurrency()
        );

        List<String> dates = new ArrayList<>();
        List<String> values = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList dayNodes = doc.getElementsByTagName("Day");

            for (int i = 0; i < dayNodes.getLength(); i++) {
                Element day = (Element) dayNodes.item(i);
                String date = day.getAttribute("date");
                String rateStr = day.getElementsByTagName("Rate").item(0).getTextContent();

                // Convert "384,67" → 384.67
                rateStr = rateStr.replace(",", ".");

                dates.add(date);
                values.add(String.valueOf(Double.parseDouble(rateStr)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        model.addAttribute("dates", dates);
        model.addAttribute("values", values);
        String strOut= "Currency:"+messagePrice.getCurrency()+";"+"Start date:"+messagePrice.getStartDate()+";"+"End date:"+messagePrice.getEndDate()+";";
        strOut+=service.getExchangeRates(messagePrice.getStartDate(),messagePrice.getEndDate(),messagePrice.getCurrency());
        model.addAttribute("sendOut", strOut);

        return "soapresult";
    }

}
