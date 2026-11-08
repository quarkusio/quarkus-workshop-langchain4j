import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import CopyButtonPlugin from 'highlightjs-copy';
import 'highlightjs-copy/dist/highlightjs-copy.min.css';

document.addEventListener('DOMContentLoaded', function() {
    turnMermaidBlocksIntoTargets();
    hljs.addPlugin(new CopyButtonPlugin());
    hljs.highlightAll();
    highlightSelectedLines();
    renderMermaid();
});

// ---------------------------------------------------------------- code lines

// The Markdown extension writes hl_lines="1-3 7" from the fence info string
// into data-hl-lines. Each line of the highlighted block is then wrapped in
// its own element so a selected line can get a full-width background, which
// the plain <code> text cannot do.
function highlightSelectedLines() {
    function parseSpec(spec) {
        var lines = new Set();
        spec.split(/[\s,]+/).forEach(function(part) {
            var range = part.match(/^(\d+)(?:-(\d+))?$/);
            if (!range) return;
            var start = parseInt(range[1], 10);
            var end = range[2] ? parseInt(range[2], 10) : start;
            for (var i = start; i <= end; i++) lines.add(i);
        });
        return lines;
    }

    function escapeText(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function openTagOf(el) {
        var tag = '<' + el.tagName.toLowerCase();
        for (var i = 0; i < el.attributes.length; i++) {
            var attr = el.attributes[i];
            tag += ' ' + attr.name + '="' + attr.value.replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '"';
        }
        return tag + '>';
    }

    // Rebuilds the highlighted markup line by line, closing and reopening
    // token elements around every newline. A block comment that highlight.js
    // rendered as one span therefore keeps its colour on each line.
    function splitLines(code) {
        var lines = [];
        var stack = [];
        var current = '';

        function closeOpen() {
            for (var i = stack.length - 1; i >= 0; i--) current += '</' + stack[i].name + '>';
        }

        function openAll() {
            for (var i = 0; i < stack.length; i++) current += stack[i].open;
        }

        function visit(node) {
            Array.prototype.forEach.call(node.childNodes, function(child) {
                if (child.nodeType === Node.TEXT_NODE) {
                    child.nodeValue.split('\n').forEach(function(part, index) {
                        if (index > 0) {
                            closeOpen();
                            lines.push(current);
                            current = '';
                            openAll();
                        }
                        current += escapeText(part);
                    });
                } else if (child.nodeType === Node.ELEMENT_NODE) {
                    var name = child.tagName.toLowerCase();
                    var open = openTagOf(child);
                    current += open;
                    stack.push({ name: name, open: open });
                    visit(child);
                    stack.pop();
                    current += '</' + name + '>';
                }
            });
        }

        visit(code);
        closeOpen();
        lines.push(current);
        return lines;
    }

    var blocks = document.querySelectorAll('code[data-hl-lines]');
    Array.prototype.forEach.call(blocks, function(code) {
        var lines = parseSpec(code.getAttribute('data-hl-lines'));
        if (lines.size === 0) return;

        var parts = splitLines(code);
        var markup = parts.map(function(line, index) {
            var number = index + 1;
            // A fence always ends with a newline. Wrapping the empty remainder
            // would add a phantom line, so leave it as plain text.
            if (line === '' && number === parts.length) {
                return '';
            }
            var classes = 'code-line' + (lines.has(number) ? ' hl-line' : '');
            return '<span class="' + classes + '">' + line + '</span>';
        });
        code.innerHTML = markup.join('\n');
    });
}

// ------------------------------------------------------------------ mermaid

// Source of every diagram is kept on the element so a theme switch can
// render it again with the matching colours.
function turnMermaidBlocksIntoTargets() {
    var blocks = document.querySelectorAll('pre > code.language-mermaid');
    Array.prototype.forEach.call(blocks, function(code) {
        var target = document.createElement('div');
        target.className = 'mermaid';
        target.dataset.mermaidSource = code.textContent;
        target.textContent = code.textContent;
        code.parentElement.replaceWith(target);
    });
}

// Mermaid is loaded from a CDN, the way the previous site did it, and only
// on pages that contain a diagram. Bundling it through Maven is not possible:
// mermaid's own dependency ranges conflict (cose-base 1.x versus 2.x), so the
// build cannot resolve it.
var MERMAID_VERSION = '11.17.2';
var MERMAID_URL = 'https://cdn.jsdelivr.net/npm/mermaid@' + MERMAID_VERSION + '/dist/mermaid.min.js';
var mermaidLoading = null;

function loadMermaid() {
    if (window.mermaid) return Promise.resolve(window.mermaid);
    if (!mermaidLoading) {
        mermaidLoading = new Promise(function(resolve, reject) {
            var script = document.createElement('script');
            script.src = MERMAID_URL;
            script.async = true;
            script.onload = function() { resolve(window.mermaid); };
            script.onerror = function() { reject(new Error('Mermaid failed to load')); };
            document.head.appendChild(script);
        });
    }
    return mermaidLoading;
}

// No CDN (offline reader, blocked host): show the diagram source as a code
// block instead of a wall of unstyled text.
function restoreMermaidBlock(target) {
    var pre = document.createElement('pre');
    var code = document.createElement('code');
    code.className = 'language-mermaid';
    code.textContent = target.dataset.mermaidSource;
    pre.appendChild(code);
    target.replaceWith(pre);
    hljs.highlightElement(code);
}

function mermaidConfig() {
    var dark = document.documentElement.classList.contains('dark');
    var fontFamily = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif";
    return {
        startOnLoad: false,
        securityLevel: 'loose',
        theme: dark ? 'dark' : 'base',
        themeVariables: dark ? {
            fontFamily: fontFamily,
            darkMode: true,
            background: '#171717',
            mainBkg: '#1f3a54',
            primaryColor: '#1f3a54',
            primaryTextColor: '#e6e6e6',
            primaryBorderColor: '#64b5f6',
            nodeBorder: '#64b5f6',
            lineColor: '#b0bec5',
            textColor: '#e6e6e6',
            secondaryColor: '#2a2a2a',
            tertiaryColor: '#242424',
            edgeLabelBackground: '#171717'
        } : {
            fontFamily: fontFamily,
            background: '#ffffff',
            mainBkg: '#e3f2fd',
            primaryColor: '#e3f2fd',
            primaryTextColor: '#212121',
            primaryBorderColor: '#1565c0',
            nodeBorder: '#1565c0',
            lineColor: '#5f6368',
            textColor: '#212121',
            secondaryColor: '#f1f8e9',
            tertiaryColor: '#ffffff',
            edgeLabelBackground: '#ffffff'
        }
    };
}

function renderMermaid() {
    var targets = document.querySelectorAll('.mermaid');
    if (targets.length === 0) return;
    loadMermaid().then(function(mermaid) {
        mermaid.initialize(mermaidConfig());
        Array.prototype.forEach.call(targets, function(target) {
            // A rendered diagram contains an SVG; put the original text back
            // before asking for a new render.
            target.textContent = target.dataset.mermaidSource;
            target.removeAttribute('data-processed');
        });
        return mermaid.run({ nodes: targets, suppressErrors: true });
    }).catch(function() {
        Array.prototype.forEach.call(targets, restoreMermaidBlock);
    });
}

// Re-render when the reader switches between light and dark.
new MutationObserver(function() {
    renderMermaid();
}).observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });

// GitHub repository stats (stars / forks) shown in the navbar.
// Uses the public GitHub API, cached in localStorage for an hour to stay
// well within the unauthenticated rate limit.
(function() {
    var KEY = 'ghRepoStats';
    var cache = null;
    try {
        cache = JSON.parse(localStorage.getItem(KEY) || 'null');
    } catch (e) {
        // ignore unavailable localStorage
    }

    function format(n) {
        if (n === undefined || n === null) return '…';
        if (n >= 1000) return (n / 1000).toFixed(n >= 10000 ? 0 : 1).replace(/\.0$/, '') + 'k';
        return String(n);
    }

    function render() {
        if (!cache) return;
        document.querySelectorAll('[data-gh="stars"]').forEach(function(el) {
            el.textContent = format(cache.stars);
        });
        document.querySelectorAll('[data-gh="forks"]').forEach(function(el) {
            el.textContent = format(cache.forks);
        });
    }

    if (cache && Date.now() - cache.fetchedAt < 3600000) {
        render();
        return;
    }

    fetch('https://api.github.com/repos/quarkusio/quarkus-workshop-langchain4j')
        .then(function(r) { return r.ok ? r.json() : null; })
        .then(function(data) {
            if (!data) return;
            cache = { stars: data.stargazers_count, forks: data.forks_count, fetchedAt: Date.now() };
            try {
                localStorage.setItem(KEY, JSON.stringify(cache));
            } catch (e) {
                // ignore unavailable localStorage
            }
            render();
        })
        .catch(function() {
            // leave the placeholder, the links still work
        });
})();

// Fullscreen lightbox for content images (click to zoom, Escape or click to close)
(function() {
    var overlay = document.createElement('div');
    overlay.className = 'ws-lightbox';
    var img = document.createElement('img');
    var caption = document.createElement('div');
    caption.className = 'ws-lightbox-caption';
    overlay.appendChild(img);
    overlay.appendChild(caption);

    function close() {
        overlay.classList.remove('open');
    }

    overlay.addEventListener('click', close);
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') close();
    });

    document.addEventListener('DOMContentLoaded', function() {
        document.body.appendChild(overlay);
        document.querySelectorAll('.content-prose img, .home-content img').forEach(function(el) {
            el.addEventListener('click', function() {
                img.src = el.src;
                img.alt = el.alt || '';
                caption.textContent = el.alt || '';
                caption.style.display = el.alt ? '' : 'none';
                overlay.classList.add('open');
            });
        });
    });
})();
