package ai.summarizer;

public class Main {
    public static void main(String[] args) {

            String text = """
                    Dubai ist eine Stadt, in der Moderne und Luxus auf einzigartige Weise miteinander verschmelzen. 
                    Glitzernde Wolkenkratzer ragen aus der Wüste empor und spiegeln den grenzenlosen Ehrgeiz dieser Metropole wider.
                    Das Leben dort ist geprägt von Komfort, Exklusivität und einem hohen Lebensstandard.
                    Traumhafte Strände mit türkisfarbenem Wasser laden zum Entspannen unter der warmen Sonne ein.
                    Luxuriöse Hotels und Resorts erfüllen selbst höchste Ansprüche.
                    In Dubai trifft man Menschen aus aller Welt, was der Stadt eine kosmopolitische Atmosphäre verleiht.
                    Moderne Einkaufszentren bieten nicht nur Shopping, sondern auch spektakuläre Erlebnisse.
                    Feinste Restaurants servieren internationale Küche auf Weltklasseniveau.
                    Das Nachtleben ist glamourös und abwechslungsreich.
                    Gleichzeitig findet man in der Wüste Ruhe, Weite und faszinierende Schönheit.
                                                               Sicherheit und Sauberkeit prägen den Alltag vieler Bewohner.
                                                               Innovationen und Zukunftsvisionen sind in Dubai allgegenwärtig.
                                                               Tradition und Kultur werden trotz aller Modernität bewahrt.
                                                               Das Leben in Dubai fühlt sich für viele wie ein dauerhaftes Urlaubserlebnis an.
                                                               So wird die Stadt für zahlreiche Menschen zum Symbol eines wahr gewordenen Traums.
                """;

            String summary = Summarizer.summarize(text, 2);

            System.out.println("SUMMARY:");
            System.out.println(summary);
    }
}