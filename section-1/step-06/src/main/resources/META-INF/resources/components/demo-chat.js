import {css, LitElement} from 'lit';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/horizontal-layout';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

export class DemoChat extends LitElement {

    _stripHtml(html)   {
        const div = document.createElement("div");
        div.innerHTML = html;
        return div.textContent || div.innerText || "";
    }

    connectedCallback() {
        const MAX_RECONNECT_ATTEMPTS = 30; // 30 attempts * 10s = 5 minutes
        const RECONNECT_INTERVAL = 10000; // 10 seconds

        const chatBot = document.getElementsByTagName("chat-bot")[0];
        const that = this;

        let socket = null;
        let reconnectAttempts = 0;
        let reconnectTimer = null;

        function clearMessages() {
            // Keep the last "Reconnected" message if it exists
            const lastMessage = chatBot.messages.length > 0 ? chatBot.messages[chatBot.messages.length - 1] : null;
            const keepLastMessage = lastMessage &&
                lastMessage.sender &&
                lastMessage.sender.name === "System" &&
                lastMessage.message &&
                lastMessage.message.includes("Reconnected");

            if (keepLastMessage) {
                // Remove all messages except the last one
                while (chatBot.messages.length > 1) {
                    chatBot.messages.shift();
                }
                // Remove all bubbles except the last one from DOM
                const bubbles = chatBot.shadowRoot.querySelectorAll("chat-bubble");
                for (let i = 0; i < bubbles.length - 1; i++) {
                    bubbles[i].remove();
                }
            } else {
                // Clear all messages
                while (chatBot.messages.length > 0) {
                    chatBot.messages.pop();
                }
                // Clear all chat bubbles from DOM
                const bubbles = chatBot.shadowRoot.querySelectorAll("chat-bubble");
                bubbles.forEach(bubble => bubble.remove());
            }
        }

        function createWebSocket() {
            const protocol = (window.location.protocol === 'https:') ? 'wss' : 'ws';
            const ws = new WebSocket(protocol + '://' + window.location.host + '/customer-support-agent');

            ws.onopen = function() {
                // If this is a reconnection, show reconnected message
                const isReconnection = reconnectAttempts > 0;

                // Clear old messages on successful connection/reconnection
                clearMessages();

                if (isReconnection) {
                    chatBot.sendMessage("✅ Reconnected", {
                        right: false,
                        sender: {
                            name: "System"
                        }
                    });

                    reconnectAttempts = 0;
                    if (reconnectTimer) {
                        clearTimeout(reconnectTimer);
                        reconnectTimer = null;
                    }
                }
            };

            ws.onmessage = function (event) {
                chatBot.hideLastLoading();
                // LLM response
                let lastMessage;
                if (chatBot.messages.length > 0) {
                    lastMessage = chatBot.messages[chatBot.messages.length - 1];
                }
                if (lastMessage && lastMessage.sender.name === "Bot"  && ! lastMessage.loading) {
                    if (! lastMessage.msg) {
                        lastMessage.msg = "";
                    }
                    lastMessage.msg += event.data;
                    let bubbles = chatBot.shadowRoot.querySelectorAll("chat-bubble");
                    let bubble = bubbles.item(bubbles.length - 1);
                    if (lastMessage.message) {
                        bubble.innerHTML = that._stripHtml(lastMessage.message) + lastMessage.msg;
                    } else {
                        bubble.innerHTML = lastMessage.msg;
                    }
                    chatBot.body.scrollTo({ top: chatBot.body.scrollHeight, behavior: 'smooth' })
                } else {
                    chatBot.sendMessage(event.data, {
                        right: false,
                        sender: {
                            name: "Bot"
                        }
                    });
                }
            };

            ws.onclose = function(event) {
                handleDisconnection();
            };

            ws.onerror = function(error) {
                console.error('WebSocket error:', error);
            };

            return ws;
        }

        function handleDisconnection() {
            // Clear old messages
            clearMessages();

            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                // Show reconnecting message only on first disconnection
                if (reconnectAttempts === 0) {
                    chatBot.sendMessage("⚠️ Connection lost - Reconnecting...", {
                        right: false,
                        sender: {
                            name: "System"
                        }
                    });
                }

                reconnectAttempts++;
                reconnectTimer = setTimeout(function() {
                    socket = createWebSocket();
                }, RECONNECT_INTERVAL);
            } else {
                // Max attempts reached, show failure message
                chatBot.sendMessage("☠️ Connection lost - Please refresh your browser", {
                    right: false,
                    sender: {
                        name: "System"
                    }
                });
            }
        }

        // Create initial connection
        socket = createWebSocket();

        chatBot.addEventListener("sent", function (e) {
            if (e.detail.message.sender.name !== "Bot" && e.detail.message.sender.name !== "System") {
                // User message
                const msg = that._stripHtml(e.detail.message.message);
                if (socket && socket.readyState === WebSocket.OPEN) {
                    socket.send(msg);
                    chatBot.sendMessage("", {
                        right: false,
                        loading: true
                    });
                }
            }
        });
    }


}

customElements.define('demo-chat', DemoChat);