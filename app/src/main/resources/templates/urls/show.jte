@import hexlet.code.model.Url
@import hexlet.code.model.UrlCheck
@import hexlet.code.dto.MainPage
@import java.util.List
@param Url url
@param MainPage page
@param List<UrlCheck> checks

@template.layout.page(
    content = @`
        @if(page != null && page.getFlash() != null && !page.getFlash().isEmpty())
            <div class="alert alert-info alert-dismissible fade show" role="alert">
                ${page.getFlash()}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif

        <h1>Сайт: ${url.getName()}</h1>

        <table class="table table-bordered" data-test="url">
            <tbody>
                <tr>
                    <th>ID</th>
                    <td>${url.getId()}</td>
                </tr>
                <tr>
                    <th>Имя</th>
                    <td>${url.getName()}</td>
                </tr>
                <tr>
                    <th>Дата создания</th>
                    <td>${url.getCreatedAt().toString()}</td>
                </tr>
            </tbody>
        </table>

        <h2 class="mt-4">Проверки</h2>
        <form action="/urls/${url.getId()}/checks" method="post" class="mb-3">
            <button type="submit" class="btn btn-primary">Запустить проверку</button>
        </form>

        <table class="table table-bordered" data-test="checks">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Код ответа</th>
                    <th>h1</th>
                    <th>title</th>
                    <th>description</th>
                    <th>Дата создания</th>
                </tr>
            </thead>
            <tbody>
                @for(var check : checks)
                    <tr>
                        <td>${check.getId()}</td>
                        <td>${check.getStatusCode()}</td>
                        <td>${check.getH1() != null ? check.getH1() : ""}</td>
                        <td>${check.getTitle() != null ? check.getTitle() : ""}</td>
                        <td>${check.getDescription() != null ? check.getDescription() : ""}</td>
                        <td>${check.getCreatedAt().toString()}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    `
)
