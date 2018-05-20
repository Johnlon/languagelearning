import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class Roots {
    String person() {return "/person";}
    String location() {return "/location";}

    String person(String id) { return person() + "/" + id; }
    String location(String id) { return location() + "/" + id; }
};

class Person {
    public String id;
    public String selfUrl;
    public String name;
    public String location;
    public String locationUrl;

    public static Person make(
            Roots roots,
            String _id,
            String _name,
            String _location
    ) {
        return new Person() {{
            this.id = _id;
            this.selfUrl = roots.person(_id);
            this.name = _name;
            this.location = _location;
            this.locationUrl = roots.location(_location);
        }};
    }
}

class Location {
    public String location;
    public String selfUrl;
    public String city;
    public String country;

    public static Location make(
            Roots roots,
            String _id,
            String _city,
            String _country
    ) {
        return new Location() {{
            this.location = _id;
            this.selfUrl = roots.location(_id);
            this.city = _city;
            this.country = _country;
        }};
    }
}

public class DataService {
    static String PERSON_ROOT = "PERSON_ROOT";
    static String LOCATION_ROOT = "PERSON_ROOT";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/person", new PersonHandler());
        server.createContext("/location", new LocationHandler());
        server.setExecutor(null); // creates a default executor

        int p = 10;
        int l = 5;

        Roots roots= new Roots();
        people = initPerson(roots, p, l);
        locations = initLocation(roots, l);

        server.start();

    }

    static Person[] people = new Person[0];
    static Location[] locations = new Location[0];

    static String[] countries = {
                "UK",
                "US",
                "FR"
        };

    private static Person[] initPerson(Roots root, int p, int l) {
        Person[] people = new Person[p];
        for (int id = 0; id < p; id++) {
            people[id] = Person.make(root, "" + id, "person" + id, "" + (id % l));
        }
        return people;
    }

    private static Location[] initLocation(Roots roots, int l) {
        Location[] locations = new Location[l];
        for (int id = 0; id < l; id++) {
            locations[id] = Location.make(roots,"" + id, "city" + id, countries[id % countries.length]);
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

                Object responseObject = getResponseObject(t, people, "/person");

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
                Object responseValue = getResponseObject(t, locations, "/location");

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
        return response.getBuffer().toString().replaceAll(root_placeholder, thisRoot);
    }

    public static Map<String, String> getQueryMap(String query)
    {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {  String [] p=param.split("=");
            String name = p[0];
            if(p.length>1)  {String value = p[1];
                map.put(name, value);
            }
        }
        return map;
    }

}