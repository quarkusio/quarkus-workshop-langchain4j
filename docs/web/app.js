import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import CopyButtonPlugin from 'highlightjs-copy';
import 'highlightjs-copy/dist/highlightjs-copy.min.css';

document.addEventListener('DOMContentLoaded', function() {
    hljs.addPlugin(new CopyButtonPlugin());
    hljs.highlightAll();
});
