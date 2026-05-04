@param String flash
@param String content

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Анализатор страниц</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="/">Анализатор страниц</a>
        </div>
    </nav>

    <main class="container mt-4">
        @if(flash != null && !flash.isEmpty())
            <div class="alert alert-info alert-dismissible fade show" role="alert">
                ${flash}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        @endif

        ${content}
    </main>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/js/bootstrap.bundle.min.js"></script>
</body>
</html>
