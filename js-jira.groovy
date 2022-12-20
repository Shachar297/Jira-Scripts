// Behvaiour .

getFieldByName('Summary').setFormValue('My cool value')

// Create a dummy HTML element and place it in your exsited html.

getFieldByName('Summary').setDescription( '<script> ' +
            "let m = document.getElementsByClassName('jira-dialog-core-content')[0]; m.style.backgroundColor = 'black';" +
            "let a = document.createElement('Div'); a.style.width = '100vw'; a.style.height = '100vh'; m.appendChild(a);" +
            "let a = document.createElement('a');  a.href = 'https://google.com'; a.innerHTML = 'Google'; m.appendChild(a);" +
            '</script>'
)


// Execute a REST API call from your behavior script.
getFieldByName('Summary').setDescription(
    '<script>' +
    'function get() {' +
    "fetch('https://api.publicapis.org/entries')" +
    " .then((response) => response.json())" +
    ".then((data) => console.log(data)); "+
    '}' +
    'console.log(get())'
    )