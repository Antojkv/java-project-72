@import hexlet.code.model.Url
@param java.util.List<Url> urls

@template(layout.jte, content = content)

@output
    @def content = `
        <div class="row">
            <div class="col-12">
                <h1>Сайты</h1>
                <table class="table table-bordered">
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
                                <td>${url.id}</td>
                                <td><a href="/urls/${url.id}">${url.name}</a></td>
                                <td>${url.createdAt}</td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
        </div>
    `
@end
