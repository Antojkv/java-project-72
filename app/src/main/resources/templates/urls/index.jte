@import java.util.List
@import hexlet.code.dto.UrlWithLastCheckDto
@import hexlet.code.dto.MainPage
@param List<UrlWithLastCheckDto> urls
@param MainPage page

@template.layout.page(
    content = @`
        @if(page != null && page.getFlash() != null && !page.getFlash().isEmpty())
            <div class="alert alert-info alert-dismissible fade show" role="alert">
                ${page.getFlash()}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif

        <h1>Сайты</h1>

        <table class="table table-bordered" data-test="urls">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Имя</th>
                    <th>Дата создания</th>
                    <th>Код ответа</th>        <!-- ✅ 4-й столбец -->
                    <th>Дата последней проверки</th> <!-- ✅ 5-й столбец -->
                </tr>
            </thead>
            <tbody>
                @for(var url : urls)
                    <tr>
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>${url.getFormattedCreatedAt()}</td>
                        <td>
                            @if(url.getLastCheckStatusCode() != null)
                                ${url.getLastCheckStatusCode()}
                            @endif
                        </td>
                        <td>
                            @if(url.getLastCheckCreatedAt() != null)
                                ${url.getFormattedLastCheckCreatedAt()}
                            @else
                                Нет проверок
                            @endif
                        </td>
                    </tr>
                @else
                    <tr>
                        <td colspan="5" class="text-center">Нет добавленных сайтов</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    `
)
