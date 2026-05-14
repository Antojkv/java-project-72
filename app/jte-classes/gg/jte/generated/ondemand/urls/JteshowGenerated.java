package gg.jte.generated.ondemand.urls;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.dto.MainPage;
import java.util.List;
@SuppressWarnings("unchecked")
@javax.annotation.processing.Generated("gg.jte.TemplateEngine")
public final class JteshowGenerated {
	public static final String JTE_NAME = "urls/show.jte";
	public static final int[] JTE_LINE_INFO = {0,0,1,2,3,4,4,4,4,4,8,8,10,10,11,11,13,13,13,16,16,18,18,18,24,24,24,28,28,28,32,32,32,38,38,38,38,54,54,56,56,56,57,57,57,58,58,58,59,59,59,60,60,60,61,61,61,63,63,66,66,66,66,66,4,5,6,6,6,6};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, Url url, MainPage page, List<UrlCheck> checks) {
		jteOutput.writeContent("\n");
		gg.jte.generated.ondemand.layout.JtepageGenerated.render(jteOutput, jteHtmlInterceptor, new gg.jte.html.HtmlContent() {
			public void writeTo(gg.jte.html.HtmlTemplateOutput jteOutput) {
				jteOutput.writeContent("\n        ");
				if (page != null && page.getFlash() != null && !page.getFlash().isEmpty()) {
					jteOutput.writeContent("\n            <div class=\"alert alert-info alert-dismissible fade show\" role=\"alert\">\n                ");
					jteOutput.setContext("div", null);
					jteOutput.writeUserContent(page.getFlash());
					jteOutput.writeContent("\n                <button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\"></button>\n            </div>\n        ");
				}
				jteOutput.writeContent("\n\n        <h1>Сайт: ");
				jteOutput.setContext("h1", null);
				jteOutput.writeUserContent(url.getName());
				jteOutput.writeContent("</h1>\n\n        <table class=\"table table-bordered\" data-test=\"url\">\n            <tbody>\n                <tr>\n                    <th>ID</th>\n                    <td>");
				jteOutput.setContext("td", null);
				jteOutput.writeUserContent(url.getId());
				jteOutput.writeContent("</td>\n                </tr>\n                <tr>\n                    <th>Имя</th>\n                    <td>");
				jteOutput.setContext("td", null);
				jteOutput.writeUserContent(url.getName());
				jteOutput.writeContent("</td>\n                </tr>\n                <tr>\n                    <th>Дата создания</th>\n                    <td>");
				jteOutput.setContext("td", null);
				jteOutput.writeUserContent(url.getCreatedAt().toString());
				jteOutput.writeContent("</td>\n                </tr>\n            </tbody>\n        </table>\n\n        <h2 class=\"mt-4\">Проверки</h2>\n        <form action=\"/urls/");
				jteOutput.setContext("form", "action");
				jteOutput.writeUserContent(url.getId());
				jteOutput.setContext("form", null);
				jteOutput.writeContent("/checks\" method=\"post\" class=\"mb-3\">\n            <button type=\"submit\" class=\"btn btn-primary\">Запустить проверку</button>\n        </form>\n\n        <table class=\"table table-bordered\">\n            <thead>\n                <tr>\n                    <th>ID</th>\n                    <th>Код ответа</th>\n                    <th>h1</th>\n                    <th>title</th>\n                    <th>description</th>\n                    <th>Дата создания</th>\n                </tr>\n            </thead>\n            <tbody>\n                ");
				for (var check : checks) {
					jteOutput.writeContent("\n                    <tr>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getId());
					jteOutput.writeContent("</td>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getStatusCode());
					jteOutput.writeContent("</td>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getH1() != null ? check.getH1() : "");
					jteOutput.writeContent("</td>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getTitle() != null ? check.getTitle() : "");
					jteOutput.writeContent("</td>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getDescription() != null ? check.getDescription() : "");
					jteOutput.writeContent("</td>\n                        <td>");
					jteOutput.setContext("td", null);
					jteOutput.writeUserContent(check.getCreatedAt().toString());
					jteOutput.writeContent("</td>\n                    </tr>\n                ");
				}
				jteOutput.writeContent("\n            </tbody>\n        </table>\n    ");
			}
		});
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		Url url = (Url)params.get("url");
		MainPage page = (MainPage)params.get("page");
		List<UrlCheck> checks = (List<UrlCheck>)params.get("checks");
		render(jteOutput, jteHtmlInterceptor, url, page, checks);
	}
}
