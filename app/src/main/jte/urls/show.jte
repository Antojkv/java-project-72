@import hexlet.code.model.Url
@param Url url
@param String flash

@template(layout.jte)

<div class="row">
    <div class="col-12">
        <h1>Сайт: ${url.getName()}</h1>
        <div class="card">
            <div class="card-body">
                <p><strong>ID:</strong> ${url.getId()}</p>
                <p><strong>Имя:</strong> ${url.getName()}</p>
                <p><strong>Дата создания:</strong> ${url.getCreatedAt().toString()}</p>
            </div>
        </div>
        <a href="/urls" class="btn btn-secondary mt-3">Назад к списку</a>
    </div>
</div>
