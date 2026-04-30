@import hexlet.code.model.Url
@param Url url

@template(layout.jte, content = content)

@output
    @def content = `
        <div class="row">
            <div class="col-12">
                <h1>Сайт: ${url.name}</h1>
                <div class="card">
                    <div class="card-body">
                        <p><strong>ID:</strong> ${url.id}</p>
                        <p><strong>Имя:</strong> ${url.name}</p>
                        <p><strong>Дата создания:</strong> ${url.createdAt}</p>
                    </div>
                </div>
                <a href="/urls" class="btn btn-secondary mt-3">Назад к списку</a>
            </div>
        </div>
    `
@end
