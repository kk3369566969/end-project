const gameArea = document.getElementById('gameArea');
const player = document.getElementById('player');
const message = document.getElementById('message');
const timeText = document.getElementById('time');
const scoreText = document.getElementById('score');
const bestText = document.getElementById('best');

let x = 70;
let y = 195;
let obstacles = [];
let timer = 0;
let score = 0;
let best = Number(localStorage.getItem('block-best') || 0);
let playing = false;
let gameTimer;
let obstacleTimer;

bestText.textContent = best;

function resetGame() {
  obstacles.forEach(obstacle => obstacle.remove());
  obstacles = [];
  x = 70;
  y = (gameArea.clientHeight - player.offsetHeight) / 2;
  timer = 0;
  score = 0;
  updatePlayer();
  updateScore();
}

function updatePlayer() {
  player.style.left = `${x}px`;
  player.style.top = `${y}px`;
}

function updateScore() {
  timeText.textContent = timer;
  scoreText.textContent = score;
  bestText.textContent = best;
}

function startGame() {
  clearInterval(gameTimer);
  clearInterval(obstacleTimer);
  resetGame();
  playing = true;
  message.classList.add('hidden');
  gameTimer = setInterval(() => { timer += 1; score = timer * 10; updateScore(); }, 1000);
  obstacleTimer = setInterval(createObstacle, 900);
}

function endGame() {
  playing = false;
  clearInterval(gameTimer);
  clearInterval(obstacleTimer);
  if (score > best) { best = score; localStorage.setItem('block-best', best); }
  updateScore();
  message.textContent = `游戏结束！得分：${score}`;
  message.classList.remove('hidden');
}

function createObstacle() {
  if (!playing) return;
  const obstacle = document.createElement('div');
  const height = 50 + Math.random() * 140;
  obstacle.className = 'obstacle';
  obstacle.style.height = `${height}px`;
  obstacle.style.left = `${gameArea.clientWidth}px`;
  obstacle.style.top = `${Math.random() * (gameArea.clientHeight - height)}px`;
  gameArea.appendChild(obstacle);
  obstacles.push(obstacle);

  let obstacleX = gameArea.clientWidth;
  const move = setInterval(() => {
    if (!playing) { clearInterval(move); return; }
    obstacleX -= 4;
    obstacle.style.left = `${obstacleX}px`;
    if (isColliding(player, obstacle)) { clearInterval(move); endGame(); }
    if (obstacleX < -30) { clearInterval(move); obstacle.remove(); obstacles = obstacles.filter(item => item !== obstacle); }
  }, 30);
}

function isColliding(first, second) {
  const a = first.getBoundingClientRect();
  const b = second.getBoundingClientRect();
  return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
}

function move(direction) {
  if (!playing) return;
  const step = 18;
  if (direction === 'up') y -= step;
  if (direction === 'down') y += step;
  if (direction === 'left') x -= step;
  if (direction === 'right') x += step;
  x = Math.max(0, Math.min(gameArea.clientWidth - player.offsetWidth, x));
  y = Math.max(0, Math.min(gameArea.clientHeight - player.offsetHeight, y));
  updatePlayer();
}

document.getElementById('startButton').addEventListener('click', startGame);
document.getElementById('restartButton').addEventListener('click', startGame);
document.querySelectorAll('.controls button').forEach(button => button.addEventListener('click', () => move(button.dataset.direction)));
document.addEventListener('keydown', event => {
  const keys = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right', w: 'up', s: 'down', a: 'left', d: 'right' };
  if (keys[event.key]) { event.preventDefault(); move(keys[event.key]); }
});

resetGame();
