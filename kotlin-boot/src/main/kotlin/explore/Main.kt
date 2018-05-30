package explore

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder


@SpringBootApplication
class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

// jvm doesn't guarantee reflected order matches class
@Target(AnnotationTarget.FIELD)
annotation class Order(val pos: Int)

data class PersonView(
        @Order(1) val id: String,
        @Order(2) val name: String,
        @Order(3) val city: String,
        @Order(4) val country: String
)

data class Person(
        val id: String,
        val selfUrl: String,
        val name: String,
        val location: String?,
        val locationUrl: String?
) {
    // used by serialisation
    constructor() : this("","","",null,null)
}

data class Location(
        val id: String,
        val city: String,
        val country: String
) {
    // used by serialisation
    constructor() : this("","","")
}

@Controller
class SearchController {

    @RequestMapping(value = ["/search"], method = [RequestMethod.GET])
    @ResponseBody
    fun search(@RequestParam(value = "name") name: String): String {

        try {
            val restTemplate = RestTemplate()
            val cache = mutableMapOf<String, Location>()
            println("SEARCH $name")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val s = "http://localhost:8000/search?name=$encodedName"
            println("Fetching Persons: $s")
            val persons = restTemplate.getForObject(s, Array<Person>::class.java)

            val results =
                    persons.map { p ->
                        val location = if (p.location == null || p.locationUrl == null) null
                        else lookupLocation(cache, p.location, p.locationUrl)
                        PersonView(p.id, p.name, location?.city ?: "unknown", location?.country ?: "unknown")
                    }

            return toHtml(results)
        } catch (ex: Exception) {
            println("EX= "+ ex)
            throw ex
        }
    }


    private fun lookupLocation(cache: MutableMap<String, Location>, location: String, locationUrl: String): Location {

        return cache.computeIfAbsent(location, { fetchLocation(locationUrl) })
    }

    private fun toHtml(people: Collection<PersonView>): String {
        return createHTML(prettyPrint = true).html {
            head {
                title("Persons")
                link(rel = "stylesheet", type = "text/css", href = "table.css")

            }

            val declaredFields = PersonView::class.java.declaredFields
            declaredFields.sortBy { it.getAnnotation(Order::class.java)?.pos ?: 1 }
            declaredFields.forEach { it.isAccessible = true }

            body {
                table {
                    thead {
                        tr {
                            //for (f in declaredFields) {
                            declaredFields.map {
                                th { +(it.name) }
                            }
                        }
                    }
                    tbody {
                        people.map { p: PersonView ->
                            tr {
                                for (f in declaredFields) {
                                    val v: String = ((f.get(p) ?: "-").toString())
                                    td {
                                        attributes["data-column"] = f.name
                                        +v
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchLocation(locationUrl: String): Location {
        val restTemplate = RestTemplate()
        println("Fetching Location $locationUrl")
        return restTemplate.getForObject(locationUrl, Location::class.java)
    }
}
