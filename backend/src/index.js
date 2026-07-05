import { buildServer } from './server.js';
import { config } from './config.js';

const server = await buildServer();

try {
  await server.listen({ port: config.port, host: config.host });
} catch (error) {
  server.log.error(error);
  process.exit(1);
}

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.once(signal, async () => {
    await server.close();
    process.exit(0);
  });
}
