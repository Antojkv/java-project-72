@import hexlet.code.dto.MainPage
@param MainPage page

@template.layout.page(
    content = @`
        @if(page != null && page.getFlash() != null && !page.getFlash().isEmpty())
            <div class="alert alert-info alert-dismissible fade show" role="alert">
                ${page.getFlash()}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif
        <div class="row">
            <div class="col-12 col-md-10 col-lg-8 mx-auto border rounded-3 bg-light p-5">
                <h1 class="display-3">Анализатор страниц</h1>
                <p class="lead">Бесплатно проверяйте сайты на SEO-пригодность</p>
                <form action="/urls" method="post" class="row">
                    <div class="col-8">
                        <label for="url-name" class="visually-hidden">Url для проверки</label>
                        <input id="url-name" type="text" name="url" class="form-control form-control-lg" placeholder="https://www.example.com" required>
                    </div>
                    <div class="col-2">
                        <button type="submit" class="btn btn-primary btn-lg ms-3 px-5 text-uppercase">Проверить</button>
                    </div>
                </form>
            </div>
        </div>
    `
)