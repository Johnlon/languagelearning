package explore

import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.server.WebHandler


fun beans() = org.springframework.context.support.beans {
    //    bean<PersonHandler>()
    bean<Routes>()
    bean<WebHandler>("webHandler") {
        RouterFunctions.toWebHandler(
                ref<Routes>().router()//,
                //HandlerStrategies.builder().viewResolver(ref()).build()
        )
    }
//    bean {
//        val prefix = "classpath:/templates/"
//        val suffix = ".mustache"
//        val loader = MustacheResourceTemplateLoader(prefix, suffix)
//        MustacheViewResolver(Mustache.compiler().withLoader(loader)).apply {
//            setPrefix(prefix)
//            setSuffix(suffix)
//        }
//    }
}