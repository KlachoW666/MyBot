import { applySchema, dialect, pool } from '../src/db/pool.js';

await applySchema();
console.log(`schema applied (${dialect})`);
await pool.end();
