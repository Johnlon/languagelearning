package explore

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux


class PersonHandler(val renderHtml: Boolean) {
    val svc = Service()
    val renderer = Renderer()

    fun search(request: ServerRequest): Mono<ServerResponse> {
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
            GET("/search", personHandlerHtml::search)
        }
        accept(APPLICATION_JSON).nest {
            GET("/search", personHandlerJson::search)
        }

        resources("/**", ClassPathResource("static/"))
    }
}
