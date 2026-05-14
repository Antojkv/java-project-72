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
                </tr>
            </thead>
            <tbody>
                @for(var url : urls)
                    <tr>
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>${url.getCreatedAt().toString()}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    `
)