import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import CopyButtonPlugin from 'highlightjs-copy';
import 'highlightjs-copy/dist/highlightjs-copy.min.css';

document.addEventListener('DOMContentLoaded', function() {
    hljs.addPlugin(new CopyButtonPlugin());
    hljs.highlightAll();
});

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
