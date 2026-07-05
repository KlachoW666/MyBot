/** Прикладная ошибка, которую роуты превращают в HTTP-ответ. */
export class AppError extends Error {
  /**
   * @param {number} statusCode HTTP-статус
   * @param {string} code машинный код (INSUFFICIENT_BALANCE, ...)
   * @param {string} message человекочитаемое сообщение
   */
  constructor(statusCode, code, message) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
  }
}

export const errorHandler = (error, request, reply) => {
  if (error instanceof AppError) {
    return reply.status(error.statusCode).send({ error: error.code, message: error.message });
  }
  // Ошибки валидации схем Fastify отдаём как есть.
  if (error.validation) {
    return reply.status(400).send({ error: 'BAD_REQUEST', message: error.message });
  }
  request.log.error({ err: error }, 'unhandled error');
  return reply.status(500).send({ error: 'INTERNAL', message: 'Internal server error' });
};
