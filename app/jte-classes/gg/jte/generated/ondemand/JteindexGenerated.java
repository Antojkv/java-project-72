package gg.jte.generated.ondemand;
import hexlet.code.dto.MainPage;
@SuppressWarnings("unchecked")
@javax.annotation.processing.Generated("gg.jte.TemplateEngine")
public final class JteindexGenerated {
	public static final String JTE_NAME = "index.jte";
	public static final int[] JTE_LINE_INFO = {0,0,1,1,1,1,1,3,3,5,5,6,6,7,7,7,8,8,23,23,23,24,24,24,1,1,1,1};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, MainPage page) {
		jteOutput.writeContent("\n");
		gg.jte.generated.ondemand.layout.JtepageGenerated.render(jteOutput, jteHtmlInterceptor, new gg.jte.html.HtmlContent() {
			public void writeTo(gg.jte.html.HtmlTemplateOutput jteOutput) {
				jteOutput.writeContent("\n        ");
				if (page.getFlash() != null && !page.getFlash().isEmpty()) {
					jteOutput.writeContent("\n            <div class=\"alert alert-info\">");
					jteOutput.setContext("div", null);
					jteOutput.writeUserContent(page.getFlash());
					jteOutput.writeContent("</div>\n        ");
				}
				jteOutput.writeContent("\n        <div class=\"row\">\n            <div class=\"col-12 col-md-10 col-lg-8 mx-auto border rounded-3 bg-light p-5\">\n                <h1 class=\"display-3\">Анализатор страниц</h1>\n                <p class=\"lead\">Бесплатно проверяйте сайты на SEO пригодность</p>\n                <form action=\"/urls\" method=\"post\" class=\"row\">\n                    <div class=\"col-8\">\n                        <input type=\"text\" name=\"url\" class=\"form-control form-control-lg\" placeholder=\"https://www.example.com\">\n                    </div>\n                    <div class=\"col-2\">\n                        <button type=\"submit\" class=\"btn btn-primary btn-lg ms-3 px-5 text-uppercase mx-3\">Проверить</button>\n                    </div>\n                </form>\n            </div>\n        </div>\n    ");
			}
		});
		jteOutput.writeContent("\n");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		MainPage page = (MainPage)params.get("page");
		render(jteOutput, jteHtmlInterceptor, page);
	}
}
