import React, { useState } from 'react';

/**
 * Настоящий артворк подарка из Telegram (thumbnail стикера через
 * backend-прокси /api/gifts/:id/image). Эмодзи — только фолбэк,
 * если картинка недоступна.
 */
export function GiftImage({ giftId, emoji, size = 48, className = '' }) {
  const [failed, setFailed] = useState(false);

  if (!giftId || failed) {
    return (
      <span className={`gift-fallback ${className}`} style={{ fontSize: size * 0.78, lineHeight: 1 }}>
        {emoji ?? '🎁'}
      </span>
    );
  }
  return (
    <img
      className={`gift-img ${className}`}
      src={`/api/gifts/${encodeURIComponent(giftId)}/image`}
      width={size}
      height={size}
      alt={emoji ?? 'gift'}
      loading="lazy"
      draggable="false"
      onError={() => setFailed(true)}
    />
  );
}
