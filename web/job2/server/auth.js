import crypto from "node:crypto";
import { query } from "./db.js";

const roleLabels = { reader: "普通读者", admin: "管理员" };

const publicUser = (user) => ({
  id: user.id,
  name: user.name,
  email: user.email,
  role: user.role,
  roleLabel: roleLabels[user.role] || "普通读者",
  bio: user.bio || "",
});

function hashPassword(password, salt = crypto.randomBytes(16).toString("hex")) {
  const hash = crypto.scryptSync(password, salt, 64).toString("hex");
  return `scrypt:${salt}:${hash}`;
}

function verifyPassword(password, stored) {
  const [algorithm, salt, expected] = String(stored || "").split(":");
  if (algorithm !== "scrypt" || !salt || !expected) return false;
  const actual = crypto.scryptSync(password, salt, 64).toString("hex");
  return crypto.timingSafeEqual(Buffer.from(actual, "hex"), Buffer.from(expected, "hex"));
}

export async function login(email, password) {
  const [rows] = await query(
    "SELECT id, name, email, password_hash, role, bio FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1",
    [String(email || "").trim()],
  );
  const user = rows[0];
  if (!user || !verifyPassword(password, user.password_hash)) return null;
  const token = crypto.randomBytes(32).toString("hex");
  await query(
    "INSERT INTO user_sessions (token, user_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 7 DAY))",
    [token, user.id],
  );
  return { token, user: publicUser(user) };
}

export async function register({ name, email, password }) {
  const normalizedEmail = String(email || "")
    .trim()
    .toLowerCase();
  const [exists] = await query("SELECT id FROM users WHERE LOWER(email) = ? LIMIT 1", [
    normalizedEmail,
  ]);
  if (exists.length) return { error: "这个邮箱已经注册过了" };
  const [result] = await query(
    "INSERT INTO users (name, email, password_hash, role, bio) VALUES (?, ?, ?, ?, ?)",
    [
      String(name).trim(),
      normalizedEmail,
      hashPassword(password),
      "reader",
      "刚来到纸灯，正在寻找第一本想读完的书。",
    ],
  );
  const [rows] = await query(
    "SELECT id, name, email, password_hash, role, bio FROM users WHERE id = ?",
    [result.insertId],
  );
  const user = rows[0];
  const token = crypto.randomBytes(32).toString("hex");
  await query(
    "INSERT INTO user_sessions (token, user_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 7 DAY))",
    [token, user.id],
  );
  return { token, user: publicUser(user) };
}

export async function getUserByToken(token) {
  if (!token) return null;
  const [rows] = await query(
    "SELECT u.id, u.name, u.email, u.role, u.bio FROM user_sessions s JOIN users u ON u.id = s.user_id WHERE s.token = ? AND s.expires_at > NOW() LIMIT 1",
    [token],
  );
  return rows[0] ? publicUser(rows[0]) : null;
}

export async function getUserFromRequest(request) {
  const token = request.headers.authorization?.replace(/^Bearer\s+/i, "");
  return getUserByToken(token);
}

export async function logout(token) {
  if (token) await query("DELETE FROM user_sessions WHERE token = ?", [token]);
}

export function hashSeedPassword(password, salt) {
  return hashPassword(password, salt);
}
