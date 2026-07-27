@import java.util.List
@import hexlet.code.model.Url
@param List<Url> urls

@template.layout.page(
    content = @`
        <h1>Сайты</h1>

        <table class="table table-bordered" data-test="urls">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Имя</th>
                    <th>Дата создания</th>
                    <th>Дата последней проверки</th>
                    <th>Код ответа</th>
                </tr>
            </thead>
            <tbody>
                @for(var url : urls)
                    <tr>
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>${url.getCreatedAt().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))}</td>
                        <td>
                            @if(url.getLastCheckCreatedAt() != null)
                                ${url.getLastCheckCreatedAt().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))}
                            @endif
                        </td>
                        <td>
                            @if(url.getLastCheckStatusCode() != null)
                                ${url.getLastCheckStatusCode()}
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
