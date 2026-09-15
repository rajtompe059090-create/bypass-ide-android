document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('test-button');
    const messageArea = document.getElementById('message-area');

    button.addEventListener('click', () => {
        messageArea.textContent = 'Action executed successfully! AI Project Builder is working.';
        messageArea.style.color = '#00e5ff';
    });
});
