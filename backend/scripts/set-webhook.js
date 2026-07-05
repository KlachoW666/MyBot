import { setWebhook } from '../src/lib/telegram-api.js';
import { config } from '../src/config.js';

if (!config.publicUrl) {
  console.error('PUBLIC_URL is not set');
  process.exit(1);
}

const result = await setWebhook({
  url: `${config.publicUrl.replace(/\/$/, '')}/bot/webhook`,
  secretToken: config.webhookSecret,
});
console.log('setWebhook:', result);
