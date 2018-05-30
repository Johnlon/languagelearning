package explore

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class SearchController {
    val svc = Service()
    val renderer = Renderer()

    @RequestMapping(value = ["/search"], method = [RequestMethod.GET])
    @ResponseBody
    fun search(@RequestParam(value = "name") name: String): String {

        try {
            val persons = svc.searchPersons(name)
            val results = svc.enrichForView(persons.toList())

            return renderer.toHtml(results)

        } catch (ex: Exception) {
            println("EX= " + ex)
            throw ex
        }
    }

}
