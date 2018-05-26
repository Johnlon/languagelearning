package explore;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class Roots {

    private static String ROOT_PLACEHOLDER = "ROOT";


    static String localise(String s, String thisRoot) {
        return s.replaceAll(ROOT_PLACEHOLDER, thisRoot);
    }

    static String searchPersonPath() {return "/search";}
    static String personPath() {return "/person";}
    static String locationPath() {return "/location";}

    static String personUrl() {return ROOT_PLACEHOLDER + personPath();}
    static String locationUrl() {return ROOT_PLACEHOLDER + locationPath();}

    static String personUrl(String id) { return id == null? null : personUrl() + "/" + id; }
    static String locationUrl(String id) { return id == null? null : locationUrl() + "/" + id; }
};
                                                                                                                                                                                                                                                                        @JsonInclude(JsonInclude.Include.NON_NULL)
class Person {
    public String id;
    public String selfUrl;
    public String name;
    public String location;
    public String locationUrl;

    public static Person make(
            String _id,
            String _name,
            String _location
    ) {
        return new Person() {{
            this.id = _id;
            this.selfUrl = Roots.personUrl(_id);
            this.name = _name;
            this.location = _location;
            this.locationUrl = Roots.locationUrl(_location);
        }};
    }
}
@JsonInclude(JsonInclude.Include.NON_NULL)
class Location {
    public String id;
    public String selfUrl;
    public String city;
    public String country;

    public static Location make(
            String _id,
            String _city,
            String _country
    ) {
        return new Location() {{
            this.id = _id;
            this.selfUrl = Roots.locationUrl(_id);
            this.city = _city;
            this.country = _country;
        }};
    }
}

public class DataService {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);

        server.createContext(Roots.searchPersonPath(), new SearchPersonHandler());
        server.createContext(Roots.personPath(), new PersonHandler());
        server.createContext(Roots.locationPath(), new LocationHandler());
        server.setExecutor(null); // creates a default executor

        int p = 10;
        int l = 5;

        Roots roots= new Roots();
        people = initPerson(roots, p, l);
        locations = initLocation(roots, l);

        System.out.println("Port 8000");
        server.start();

    }

    static Person[] people = new Person[0];
    static Location[] locations = new Location[0];

    static String[] countries = {
                "UK",
                "US",
                "FR"
        };


    static String[] names= {
            "John",
            "Katie",
            "Gracie"
    };

    private static Person[] initPerson(Roots root, int p, int l) {
        Person[] people = new Person[p];
        for (int id = 0; id < p; id++) {

            String location = id % l == 0? null : "" + id % l; // set some locations to null/unknown
            people[id] = Person.make("" + id, names[id%names.length] + id, location);
        }
        return people;
    }

    private static Location[] initLocation(Roots roots, int l) {
        Location[] locations = new Location[l];
        for (int id = 0; id < l; id++) {
            locations[id] = Location.make("" + id, "city" + id, countries[id % countries.length]);
        }
        return locations;
    }

    static class PersonHandler implements HttpHandler {

        ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange t) throws IOException {

            t.getResponseHeaders().add("content-type", "application/json");
            OutputStream os = t.getResponseBody();
            try {

                Object responseObject = getResponseObject(t, people, Roots.personPath());

                StringWriter response = new StringWriter(100);
                mapper.writeValue(response, responseObject);

                String b = localise(t, response);

                t.sendResponseHeaders(200, b.length());
                os.write(b.toString().getBytes("UTF-8"));

            } catch(Exception ex) {
                System.out.println("EX " + ex);
                throw ex;
            } finally {

                os.close();
            }
        }
    }

    static class SearchPersonHandler implements HttpHandler {

        ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange t) throws IOException {

            t.getResponseHeaders().add("content-type", "application/json");
            OutputStream os = t.getResponseBody();

            try {
                Map<String, String> params = getQueryMap(t.getRequestURI().getQuery());
                String filter = params.get("name");
                Person[] responseObject = Stream.of(DataService.people).filter(x-> filter == null || x.name.contains(filter)).toArray(Person[]::new);

                StringWriter response = new StringWriter(100);
                mapper.writeValue(response, responseObject);

                String b = localise(t, response);

                t.sendResponseHeaders(200, b.length());
                os.write(b.toString().getBytes("UTF-8"));

            } catch(Exception ex) {
                System.out.println("EX " + ex);
                throw ex;
            } finally {

                os.close();
            }
        }
    }


    static class LocationHandler implements HttpHandler {
        ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("content-type", "application/json");

            OutputStream os = t.getResponseBody();
            try {
                Object responseValue = getResponseObject(t, locations, Roots.locationPath());

                StringWriter response = new StringWriter(100);
                mapper.writeValue(response, responseValue);

                String b = localise(t, response);

                t.sendResponseHeaders(200, b.length());
                os.write(b.toString().getBytes("UTF-8"));

            } catch(Exception ex) {
                System.out.println("EX " + ex);
                throw ex;
            } finally {

                os.close();
            }
        }
    }
    private static Object getResponseObject(HttpExchange t, Object[] values, String target) {
        String path = t.getRequestURI().getPath();

        if (path.startsWith(target)) {
            String key = path.replace(target, "").replace("/", "");

            Object responseValue;
            if (key.length() > 0) {
                Integer personId = Integer.valueOf(key);
                responseValue = values[personId];
            } else {
                responseValue = values;
            }

            return responseValue;
        } else {
            throw new RuntimeException("Path " + path + " did not start with " + target);
        }
    }

    private static String localise(HttpExchange t, StringWriter response) {
        String thisRoot = "http://" + t.getRequestHeaders().getFirst("host");
        return Roots.localise(response.getBuffer().toString(), thisRoot);
    }

    public static Map<String, String> getQueryMap(String query)
    {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {  String [] p=param.split("=");
            String name = p[0];
            if(p.length>1)  {
                String value = p[1];
                map.put(name, value);
            } else {
                map.put(name, null);
            }
        }
        return map;
    }

}