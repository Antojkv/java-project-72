@import hexlet.code.model.Url
@param java.util.List<Url> urls
@param String flash

@template(layout.jte)

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
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>${url.getCreatedAt().toString()}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    </div>
</div>