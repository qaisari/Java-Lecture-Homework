package com.example.lectureHomework;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeSpecifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.oanda.v20.instrument.CandlestickGranularity.*;
import static com.oanda.v20.instrument.CandlestickGranularity.M;
import static com.oanda.v20.instrument.CandlestickGranularity.W;

@Controller
public class HomeController {
    Context ctx = new Context(Config.URL, Config.TOKEN);

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        model.addAttribute("uri", request.getRequestURI());
        return "index";
    }

    @GetMapping("/soap")
    public String soap(Model model, HttpServletRequest request) {
        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("param", new MessagePrice());
        return "soapform";
    }
    @PostMapping("/soap")
    public String soap2(@ModelAttribute MessagePrice messagePrice, Model model, HttpServletRequest request) throws
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

        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("dates", dates);
        model.addAttribute("values", values);
        String strOut= "Currency:"+messagePrice.getCurrency()+";"+"Start date:"+messagePrice.getStartDate()+";"+"End date:"+messagePrice.getEndDate()+";";
        strOut+=service.getExchangeRates(messagePrice.getStartDate(),messagePrice.getEndDate(),messagePrice.getCurrency());
        model.addAttribute("sendOut", strOut);

        return "soapresult";
    }

    @GetMapping("/forexaccount")
    public String f1(Model model, HttpServletRequest request) {
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            // Put raw object on model (template can access properties if available)
            model.addAttribute("account", summary);
            // Also add a safe string representation and try to extract a few common fields using reflection
            String accountStr = (summary != null) ? summary.toString() : "";
            model.addAttribute("accountStr", accountStr);

            if (summary != null) {
                // Attempt to read common getters if present
                try {
                    Method m;
                    m = summary.getClass().getMethod("getId");
                    Object id = m.invoke(summary);
                    model.addAttribute("accountId", id);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getCurrency");
                    Object currency = m.invoke(summary);
                    model.addAttribute("accountCurrency", currency);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getBalance");
                    Object balance = m.invoke(summary);
                    model.addAttribute("accountBalance", balance);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getMarginRate");
                    Object marginRate = m.invoke(summary);
                    model.addAttribute("accountMarginRate", marginRate);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getUnrealizedPL");
                    Object upl = m.invoke(summary);
                    model.addAttribute("accountUnrealizedPL", upl);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getMarginAvailable");
                    Object ma = m.invoke(summary);
                    model.addAttribute("accountMarginAvailable", ma);
                } catch (Exception ignored) {}

                try {
                    Method m = summary.getClass().getMethod("getOpenPositionCount");
                    Object opc = m.invoke(summary);
                    model.addAttribute("accountOpenPositions", opc);
                } catch (Exception ignored) {}
            }
            model.addAttribute("uri", request.getRequestURI());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("accountStr", "Error: " + e.getMessage());
        }
        return "forexAccount";
    }

    @GetMapping("/forexactprice")
    public String actual_prices(Model model, HttpServletRequest request) {
        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("par", new MessageActPrice());
        return "form_actual_prices";
    }

    @PostMapping("/forexactprice")
    public String actual_prices2(@ModelAttribute MessageActPrice messageActPrice, Model model, HttpServletRequest req) {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        String strOut="";
        List<String> instruments = new ArrayList<>();
        instruments.add(messageActPrice.getInstrument());

        try {
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);

            // Extract price data in a cleaner format
            List<String> askPrices = new ArrayList<>();
            List<String> bidPrices = new ArrayList<>();
            String instrument = "";
            String timestamp = "";

            for (ClientPrice price : resp.getPrices()) {
                strOut += price + "<br>";
                instrument = price.getInstrument().toString();

                // Try to extract bid/ask prices if available
                try {
                    if (price.getBids() != null && !price.getBids().isEmpty()) {
                        bidPrices.add(price.getBids().get(0).getPrice().toString());
                    }
                    if (price.getAsks() != null && !price.getAsks().isEmpty()) {
                        askPrices.add(price.getAsks().get(0).getPrice().toString());
                    }
                    if (price.getTime() != null) {
                        timestamp = price.getTime().toString();
                    }
                } catch (Exception ignored) {}
            }

            model.addAttribute("askPrices", askPrices);
            model.addAttribute("bidPrices", bidPrices);
            model.addAttribute("timestamp", timestamp);

        } catch (Exception e) {
            e.printStackTrace();
            strOut = "Error retrieving prices: " + e.getMessage();
        }

        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("price", strOut);
        return "result_actual_prices";
    }

    @GetMapping("/forexhistprice")
    public String hist_prices(Model model, HttpServletRequest request) {
        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("param", new MessageHistPrice());
        return "form_hist_prices";
    }

    @PostMapping("/forexhistprice")
    public String hist_prices2(@ModelAttribute MessageHistPrice messageHistPrice, Model model, HttpServletRequest req) {
        String strOut;
        List<String> timestamps = new ArrayList<>();
        List<String> closePrices = new ArrayList<>();
        List<String> openPrices = new ArrayList<>();
        List<String> highPrices = new ArrayList<>();
        List<String> lowPrices = new ArrayList<>();

        try {
            InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(messageHistPrice.getInstrument()));
            switch (messageHistPrice.getGranularity()) {
                case "M1": request.setGranularity(M1); break;
                case "H1": request.setGranularity(H1); break;
                case "D": request.setGranularity(D); break;
                case "W": request.setGranularity(W); break;
                case "M": request.setGranularity(M); break;
            }
            request.setCount(Long.valueOf(10));
            InstrumentCandlesResponse resp = ctx.instrument.candles(request);
            strOut = "";

            for (Candlestick candle : resp.getCandles()) {
                String time = candle.getTime() != null ? candle.getTime().toString() : "";
                timestamps.add(time);

                if (candle.getMid() != null) {
                    closePrices.add(candle.getMid().getC() != null ? candle.getMid().getC().toString() : "0");
                    openPrices.add(candle.getMid().getO() != null ? candle.getMid().getO().toString() : "0");
                    highPrices.add(candle.getMid().getH() != null ? candle.getMid().getH().toString() : "0");
                    lowPrices.add(candle.getMid().getL() != null ? candle.getMid().getL().toString() : "0");
                }

                strOut += time + "\tClose: " + candle.getMid().getC() + " Open: " + candle.getMid().getO() +
                         " High: " + candle.getMid().getH() + " Low: " + candle.getMid().getL() + ";<br>";
            }

            model.addAttribute("timestamps", timestamps);
            model.addAttribute("closePrices", closePrices);
            model.addAttribute("openPrices", openPrices);
            model.addAttribute("highPrices", highPrices);
            model.addAttribute("lowPrices", lowPrices);

        } catch (Exception e) {
            e.printStackTrace();
            strOut = "Error retrieving historical prices: " + e.getMessage();
        }

        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("price", strOut);
        return "result_hist_prices";
    }

    @GetMapping("/openposition")
    public String open_position(Model model, HttpServletRequest request) {
        model.addAttribute("uri", request.getRequestURI().toLowerCase());
        model.addAttribute("param", new MessageOpenPosition());
        return "form_open_position";
    }
    @PostMapping("/openposition")
    public String open_position2(@ModelAttribute MessageOpenPosition messageOpenPosition, Model model, HttpServletRequest req) {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        String strOut;
        String tradePrice = "";
        String tradeTime = "";

        try {
            InstrumentName instrument = new InstrumentName(messageOpenPosition.getInstrument());
            OrderCreateRequest request = new OrderCreateRequest(Config.ACCOUNTID);
            MarketOrderRequest marketorderrequest = new MarketOrderRequest();
            marketorderrequest.setInstrument(instrument);
            marketorderrequest.setUnits(messageOpenPosition.getUnits());
            request.setOrder(marketorderrequest);
            OrderCreateResponse response = ctx.order.create(request);

            strOut = "tradeId: " + response.getOrderFillTransaction().getId();

            // Extract additional details if available
            try {
                if (response.getOrderFillTransaction().getPrice() != null) {
                    tradePrice = response.getOrderFillTransaction().getPrice().toString();
                }
                if (response.getOrderFillTransaction().getTime() != null) {
                    tradeTime = response.getOrderFillTransaction().getTime().toString();
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
            strOut = "Error opening position: " + e.getMessage();
        }

        model.addAttribute("uri", req.getRequestURI().toLowerCase());
        model.addAttribute("instr", messageOpenPosition.getInstrument());
        model.addAttribute("units", messageOpenPosition.getUnits());
        model.addAttribute("id", strOut);
        model.addAttribute("tradePrice", tradePrice);
        model.addAttribute("tradeTime", tradeTime);

        return "result_open_position";
    }

    @GetMapping("/positions")
    public String positions(Model model, HttpServletRequest request) {
        Context ctx = new Context(Config.URL, Config.TOKEN);

        List<String> tradeIds = new ArrayList<>();
        List<String> instruments = new ArrayList<>();
        List<String> openTimes = new ArrayList<>();
        List<String> units = new ArrayList<>();
        List<String> prices = new ArrayList<>();
        List<String> unrealizedPLs = new ArrayList<>();

        String errorMessage = null;
        int totalPositions = 0;
        double totalUnrealizedPL = 0.0;

        try {
            List<Trade> trades = ctx.trade.listOpen(Config.ACCOUNTID).getTrades();
            totalPositions = trades.size();

            for (Trade trade : trades) {
                tradeIds.add(trade.getId() != null ? trade.getId().toString() : "-");
                instruments.add(trade.getInstrument() != null ? trade.getInstrument().toString() : "-");
                openTimes.add(trade.getOpenTime() != null ? trade.getOpenTime().toString() : "-");

                String unitsStr = trade.getCurrentUnits() != null ? trade.getCurrentUnits().toString() : "0";
                units.add(unitsStr);

                String priceStr = trade.getPrice() != null ? trade.getPrice().toString() : "0";
                prices.add(priceStr);

                String plStr = trade.getUnrealizedPL() != null ? trade.getUnrealizedPL().toString() : "0";
                unrealizedPLs.add(plStr);

                // Calculate total P/L
                try {
                    double pl = Double.parseDouble(plStr.replace(",", "."));
                    totalUnrealizedPL += pl;
                } catch (NumberFormatException ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "Error retrieving positions: " + e.getMessage();
        }

        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("tradeIds", tradeIds);
        model.addAttribute("instruments", instruments);
        model.addAttribute("openTimes", openTimes);
        model.addAttribute("units", units);
        model.addAttribute("prices", prices);
        model.addAttribute("unrealizedPLs", unrealizedPLs);
        model.addAttribute("totalPositions", totalPositions);
        model.addAttribute("totalUnrealizedPL", String.format("%.2f", totalUnrealizedPL));
        model.addAttribute("errorMessage", errorMessage);

        return "positions";
    }

    @GetMapping("/closeposition")
    public String close_position(Model model, HttpServletRequest request) {
        Context ctx = new Context(Config.URL, Config.TOKEN);

        List<String> tradeIds = new ArrayList<>();
        List<String> instruments = new ArrayList<>();
        List<String> units = new ArrayList<>();
        List<String> unrealizedPLs = new ArrayList<>();

        try {
            List<Trade> trades = ctx.trade.listOpen(Config.ACCOUNTID).getTrades();

            for (Trade trade : trades) {
                tradeIds.add(trade.getId() != null ? trade.getId().toString() : "-");
                instruments.add(trade.getInstrument() != null ? trade.getInstrument().toString() : "-");
                units.add(trade.getCurrentUnits() != null ? trade.getCurrentUnits().toString() : "0");
                unrealizedPLs.add(trade.getUnrealizedPL() != null ? trade.getUnrealizedPL().toString() : "0");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("uri", request.getRequestURI().toLowerCase());
        model.addAttribute("param", new MessageClosePosition());
        model.addAttribute("tradeIds", tradeIds);
        model.addAttribute("instruments", instruments);
        model.addAttribute("units", units);
        model.addAttribute("unrealizedPLs", unrealizedPLs);

        return "form_close_position";
    }
    @PostMapping("/closeposition")
    public String close_position2(@ModelAttribute MessageClosePosition messageClosePosition, Model model, HttpServletRequest request) {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        String tradeId = messageClosePosition.getTradeId() + "";
        String strOut = "Closed tradeId= " + tradeId;
        String closedInstrument = "";
        String closedUnits = "";
        String realizedPL = "";
        String closePrice = "";
        boolean success = false;

        try {
            com.oanda.v20.trade.TradeCloseResponse response = ctx.trade.close(new TradeCloseRequest(Config.ACCOUNTID, new TradeSpecifier(tradeId)));
            success = true;

            // Extract details from response
            if (response.getOrderFillTransaction() != null) {
                if (response.getOrderFillTransaction().getInstrument() != null) {
                    closedInstrument = response.getOrderFillTransaction().getInstrument().toString();
                }
                if (response.getOrderFillTransaction().getUnits() != null) {
                    closedUnits = response.getOrderFillTransaction().getUnits().toString();
                }
                if (response.getOrderFillTransaction().getPl() != null) {
                    realizedPL = response.getOrderFillTransaction().getPl().toString();
                }
                if (response.getOrderFillTransaction().getPrice() != null) {
                    closePrice = response.getOrderFillTransaction().getPrice().toString();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            strOut = "Error closing position: " + e.getMessage();
            success = false;
        }

        model.addAttribute("uri", request.getRequestURI().toLowerCase());
        model.addAttribute("tradeId", tradeId);
        model.addAttribute("message", strOut);
        model.addAttribute("closedInstrument", closedInstrument);
        model.addAttribute("closedUnits", closedUnits);
        model.addAttribute("realizedPL", realizedPL);
        model.addAttribute("closePrice", closePrice);
        model.addAttribute("success", success);

        return "result_close_position";
    }
}
