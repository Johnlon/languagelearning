package explore

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.net.URLEncoder


class PersonHandler(val renderHtml: Boolean) {
    val svc = Service()
    val renderer = Renderer()

    fun listPeople(request: ServerRequest): Mono<ServerResponse> {
        val name = request.queryParam("name").orElse("")
        try {
            println("SEARCH $name")

            val persons = svc.searchPersons(name)
            val results = svc.enrichForView(persons.toList())

            if (renderHtml) {
                val html = renderer.toHtml(results)
                return ServerResponse.ok().contentType(TEXT_HTML).syncBody(html)
            } else {
                return ServerResponse.ok().contentType(APPLICATION_JSON).body(results.toFlux(), PersonView::class.java)
            }
        } catch (ex: Exception) {
            println("EX= $ex")
            throw ex
        }
    }
}


class Routes {

    val personHandlerHtml =  PersonHandler(renderHtml = true)
    val personHandlerJson =  PersonHandler(renderHtml = false)

    fun router() = router {
        accept(TEXT_HTML).nest {
            GET("/search", personHandlerHtml::listPeople)
        }
        accept(APPLICATION_JSON).nest {
            GET("/search", personHandlerJson::listPeople)
        }

        resources("/static/**", ClassPathResource("static/"))
    }
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
    constructor() : this("", "", "", null, null)
}

data class Location(
        val id: String,
        val city: String,
        val country: String
) {
    // used by serialisation
    constructor() : this("", "", "")
}


class Service {

    fun enrichForView(persons: Collection<Person>): List<PersonView> {
        val cache = mutableMapOf<String, Location?>()
        val results =
                persons.map { p ->
                    val location = if (p.location == null || p.locationUrl == null) null
                    else lookupLocation(cache, p.location, p.locationUrl)

                    PersonView(p.id, p.name, location?.city ?: "unknown", location?.country ?: "unknown")
                }
        return results
    }

    fun searchPersons(name: String): Array<Person> {
        val restTemplate = RestTemplate()
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val s = "http://localhost:8000/search?name=$encodedName"
        println("Fetching Persons: $s")
        val persons = restTemplate.getForObject<Array<Person>>(s)
        return persons?: emptyArray()
    }


    private fun lookupLocation(cache: MutableMap<String, Location?>, location: String, locationUrl: String): Location? {

        return cache.computeIfAbsent(location, {
            fetchLocation(locationUrl)
        })
    }

    private fun fetchLocation(locationUrl: String): Location? {
        val restTemplate = RestTemplate()
        println("Fetching Location $locationUrl")
        return restTemplate.getForObject(locationUrl, Location::class.java)
    }

}

class Renderer {
    fun toHtml(people: Collection<PersonView>): String {
        return createHTML(prettyPrint = true).html {
            head {
                title("Persons")
                link(rel = "stylesheet", type = "text/css", href = "/static/table.css")

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
}
