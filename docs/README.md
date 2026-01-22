# Generate and expose docs locally

```bash
pip install pipenv
pipenv install
pipenv run mkdocs build --clean
pipenv run mkdocs serve --livereload
```

The doc should be available at http://127.0.0.1:8000/