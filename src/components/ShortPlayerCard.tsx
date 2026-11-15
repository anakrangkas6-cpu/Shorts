import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Bookmark,
  Expand,
  MessageSquareText,
  Pause,
  Play,
  Shrink,
  SlidersHorizontal,
  ThumbsUp,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { ShortVideoItem } from '../types/shorts';
import {
  buildCssFilter,
  formatCompactCount,
  formatDuration,
  getPresetLabel,
} from '../utils/effects';
import { ProceduralCanvasReel } from './ProceduralCanvasReel';
import { VideoEffectOverlays } from './VideoEffectOverlays';

interface HeartBurst {
  id: number;
  x: number;
  y: number;
}

interface ShortPlayerCardProps {
  video: ShortVideoItem;
  isActive: boolean;
  isAdjacent: boolean;
  isMuted: boolean;
  onToggleMute: () => void;
  onToggleLike: (videoId: string, forceLike?: boolean) => void;
  onToggleBookmark: (videoId: string) => void;
  onOpenComments: (videoId: string) => void;
  onOpenEffects: (videoId: string) => void;
  onOpenProfile: () => void;
  onDurationDetected?: (videoId: string, duration: number) => void;
}

export const ShortPlayerCard: React.FC<ShortPlayerCardProps> = ({
  video,
  isActive,
  isAdjacent,
  isMuted,
  onToggleMute,
  onToggleLike,
  onToggleBookmark,
  onOpenComments,
  onOpenEffects,
  onOpenProfile,
  onDurationDetected,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const frameRef = useRef<HTMLDivElement | null>(null);
  const tapTimerRef = useRef<number | null>(null);
  const lastTapRef = useRef<number>(0);

  const [isPlaying, setIsPlaying] = useState(false);
  const [userPaused, setUserPaused] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(video.duration || 15);
  const [fitContain, setFitContain] = useState(false);
  const [descExpanded, setDescExpanded] = useState(false);
  const [isScrubbing, setIsScrubbing] = useState(false);
  const [tapFeedback, setTapFeedback] = useState<'play' | 'pause' | null>(null);
  const [hearts, setHearts] = useState<HeartBurst[]>([]);

  const cssFilter = useMemo(() => buildCssFilter(video.effect), [video.effect]);

  // Sync play/pause when active slide changes
  useEffect(() => {
    const el = videoRef.current;

    if (!isActive) {
      setUserPaused(false);
      setIsPlaying(false);
      if (el) {
        el.pause();
        el.currentTime = 0;
      }
      setCurrentTime(0);
      return;
    }

    if (video.sourceType === 'sample') {
      setIsPlaying(!userPaused);
      return;
    }

    if (!el) return;
    el.playbackRate = video.effect.playbackRate || 1;
    el.muted = isMuted;

    if (!userPaused) {
      const playPromise = el.play();
      if (playPromise !== undefined) {
        playPromise
          .then(() => {
            setIsPlaying(true);
          })
          .catch(() => {
            // Autoplay with sound might be blocked before interaction; mute and retry
            el.muted = true;
            if (!isMuted) {
              onToggleMute();
            }
            el.play()
              .then(() => setIsPlaying(true))
              .catch(() => setIsPlaying(false));
          });
      }
    } else {
      el.pause();
      setIsPlaying(false);
    }
  }, [isActive, userPaused, video.sourceType, video.effect.playbackRate, isMuted, onToggleMute]);

  // Keep muted & playbackRate synced dynamically
  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    el.muted = isMuted;
    el.playbackRate = video.effect.playbackRate || 1;
  }, [isMuted, video.effect.playbackRate]);

  const showTransientFeedback = useCallback((kind: 'play' | 'pause') => {
    setTapFeedback(kind);
    window.setTimeout(() => {
      setTapFeedback((prev) => (prev === kind ? null : prev));
    }, 520);
  }, []);

  const togglePlayPause = useCallback(() => {
    if (video.sourceType === 'sample') {
      setUserPaused((prev) => {
        const nextPaused = !prev;
        setIsPlaying(!nextPaused);
        showTransientFeedback(nextPaused ? 'pause' : 'play');
        return nextPaused;
      });
      return;
    }

    const el = videoRef.current;
    if (!el) return;
    if (el.paused) {
      setUserPaused(false);
      el.play()
        .then(() => {
          setIsPlaying(true);
          showTransientFeedback('play');
        })
        .catch(() => {});
    } else {
      setUserPaused(true);
      el.pause();
      setIsPlaying(false);
      showTransientFeedback('pause');
    }
  }, [video.sourceType, showTransientFeedback]);

  const spawnHeart = useCallback((clientX: number, clientY: number) => {
    const rect = frameRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = clientX - rect.left;
    const y = clientY - rect.top;
    const id = Date.now() + Math.random();
    setHearts((prev) => [...prev, { id, x, y }]);
    window.setTimeout(() => {
      setHearts((prev) => prev.filter((h) => h.id !== id));
    }, 800);
  }, []);

  const handleSurfacePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    // Ignore taps on buttons or progress bar
    if ((e.target as HTMLElement).closest('button, input, a, [data-interactive="true"]')) {
      return;
    }

    const now = Date.now();
    const delta = now - lastTapRef.current;
    lastTapRef.current = now;

    if (delta < 260) {
      if (tapTimerRef.current) {
        window.clearTimeout(tapTimerRef.current);
        tapTimerRef.current = null;
      }
      spawnHeart(e.clientX, e.clientY);
      onToggleLike(video.id, true);
    } else {
      tapTimerRef.current = window.setTimeout(() => {
        togglePlayPause();
        tapTimerRef.current = null;
      }, 240);
    }
  };

  const handleScrub = (clientX: number, barEl: HTMLDivElement) => {
    const rect = barEl.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const targetTime = ratio * (duration || 1);
    setCurrentTime(targetTime);
    if (videoRef.current && video.sourceType !== 'sample') {
      videoRef.current.currentTime = targetTime;
    }
  };

  const progressPct =
    duration > 0 ? Math.min(100, Math.max(0, (currentTime / duration) * 100)) : 0;

  const sourceLabel =
    video.sourceType === 'folder'
      ? 'Folder Pribadi'
      : video.sourceType === 'uploaded'
        ? 'Studio Lokal'
        : 'Contoh Bawaan';

  const presetLabel = getPresetLabel(video.effect.preset);

  return (
    <div className="shorts-snap-item relative flex h-full w-full shrink-0 items-center justify-center lg:py-3">
      <div
        ref={frameRef}
        onPointerUp={handleSurfacePointerUp}
        className="relative h-full w-full max-w-[460px] overflow-hidden bg-zinc-950 select-none lg:aspect-[9/16] lg:h-[calc(100dvh-2rem)] lg:max-h-[880px] lg:w-auto lg:rounded-2xl lg:border lg:border-white/10 lg:shadow-2xl"
      >
        {/* Media Viewport */}
        <div
          className="absolute inset-0 flex items-center justify-center bg-black"
          style={{ filter: cssFilter }}
        >
          {video.sourceType === 'sample' && video.sampleTheme ? (
            <ProceduralCanvasReel
              theme={video.sampleTheme}
              isPlaying={isActive && isPlaying}
              playbackRate={video.effect.playbackRate}
              currentTime={currentTime}
              duration={duration}
              onTimeUpdate={setCurrentTime}
            />
          ) : video.videoUrl ? (
            <video
              ref={videoRef}
              src={video.videoUrl}
              playsInline
              loop
              muted={isMuted}
              preload={isActive || isAdjacent ? 'auto' : 'metadata'}
              className={`h-full w-full transition-[object-fit] duration-150 ${
                fitContain ? 'object-contain' : 'object-cover'
              }`}
              onLoadedMetadata={(e) => {
                const dur = e.currentTarget.duration;
                if (Number.isFinite(dur) && dur > 0) {
                  setDuration(dur);
                  onDurationDetected?.(video.id, dur);
                }
              }}
              onTimeUpdate={(e) => {
                if (!isScrubbing) {
                  setCurrentTime(e.currentTarget.currentTime);
                }
              }}
            />
          ) : (
            <div className="flex h-full w-full flex-col items-center justify-center bg-zinc-900 px-6 text-center text-zinc-400">
              <p className="text-sm font-medium text-zinc-200">
                Video tidak tersedia sementara
              </p>
              <p className="mt-1 text-xs text-zinc-400">
                Hubungkan ulang folder pribadi untuk memutar berkas ini.
              </p>
            </div>
          )}
        </div>

        {/* Video Effect Overlays */}
        <VideoEffectOverlays effect={video.effect} />

        {/* Top Measured Scrim & Minimal Controls */}
        <div className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-black/65 via-black/25 to-transparent" />
        <div className="absolute inset-x-3 top-3 z-20 flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={onToggleMute}
              aria-label={isMuted ? 'Nyalakan suara' : 'Bisukan suara'}
              className="flex h-11 w-11 items-center justify-center rounded-full bg-black/40 text-white backdrop-blur-md transition hover:bg-black/60 active:scale-95 focus-visible:outline-2 focus-visible:outline-white"
            >
              {isMuted ? <VolumeX className="h-5 w-5" /> : <Volume2 className="h-5 w-5" />}
            </button>

            {video.sourceType !== 'sample' && (
              <button
                type="button"
                onClick={() => setFitContain((prev) => !prev)}
                aria-label={fitContain ? 'Penuhi layar' : 'Sesuaikan rasio asli'}
                title={fitContain ? 'Penuhi layar' : 'Tampilkan rasio asli'}
                className="flex h-11 w-11 items-center justify-center rounded-full bg-black/40 text-white backdrop-blur-md transition hover:bg-black/60 active:scale-95 focus-visible:outline-2 focus-visible:outline-white"
              >
                {fitContain ? (
                  <Expand className="h-4 w-4" />
                ) : (
                  <Shrink className="h-4 w-4" />
                )}
              </button>
            )}
          </div>

          <button
            type="button"
            onClick={() => onOpenEffects(video.id)}
            aria-label="Atur efek dan kecepatan video"
            className="flex h-11 items-center gap-2 rounded-full bg-black/40 px-3.5 text-xs font-medium text-white backdrop-blur-md transition hover:bg-black/60 active:scale-95 focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0"
          >
            <SlidersHorizontal className="h-4 w-4" />
            <span>
              {video.effect.playbackRate !== 1
                ? `${video.effect.playbackRate}x`
                : presetLabel}
            </span>
          </button>
        </div>

        {/* Center Transient Play/Pause Feedback */}
        {tapFeedback && (
          <div className="pointer-events-none absolute left-1/2 top-1/2 z-20 -translate-x-1/2 -translate-y-1/2 rounded-full bg-black/60 p-4 text-white backdrop-blur-md">
            {tapFeedback === 'play' ? (
              <Play className="h-8 w-8 fill-white" />
            ) : (
              <Pause className="h-8 w-8 fill-white" />
            )}
          </div>
        )}

        {/* Double-tap Heart Burst Effects */}
        {hearts.map((heart) => (
          <div
            key={heart.id}
            style={{ left: heart.x, top: heart.y }}
            className="animate-heart-burst pointer-events-none absolute z-30 text-[#FF2D55] drop-shadow-[0_6px_16px_rgba(255,45,85,0.55)]"
          >
            <svg
              viewBox="0 0 24 24"
              className="h-20 w-20 fill-current"
              aria-hidden="true"
            >
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
            </svg>
          </div>
        ))}

        {/* Bottom Scrim for High Legibility */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-64 bg-gradient-to-t from-black/90 via-black/50 to-transparent" />

        {/* Right Action Rail (Shorts Style) */}
        <div className="absolute bottom-6 right-2.5 z-20 flex flex-col items-center gap-3.5 sm:right-3">
          {/* Like */}
          <button
            type="button"
            onClick={() => onToggleLike(video.id)}
            aria-label={video.likedByMe ? 'Batal suka' : 'Sukai video'}
            className="group flex flex-col items-center gap-1 focus-visible:outline-none"
          >
            <span
              className={`flex h-12 w-12 items-center justify-center rounded-full backdrop-blur-md transition duration-150 active:scale-90 ${
                video.likedByMe
                  ? 'bg-[#FF2D55] text-white shadow-lg shadow-[#FF2D55]/30'
                  : 'bg-zinc-900/65 text-white hover:bg-zinc-800/80'
              }`}
            >
              <ThumbsUp
                className={`h-5 w-5 transition-transform group-hover:scale-105 ${
                  video.likedByMe ? 'fill-white' : ''
                }`}
              />
            </span>
            <span className="tabular-nums text-[12px] font-semibold text-white drop-shadow">
              {formatCompactCount(video.likesCount)}
            </span>
          </button>

          {/* Comments */}
          <button
            type="button"
            onClick={() => onOpenComments(video.id)}
            aria-label="Buka komentar"
            className="group flex flex-col items-center gap-1 focus-visible:outline-none"
          >
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-900/65 text-white backdrop-blur-md transition duration-150 hover:bg-zinc-800/80 active:scale-90">
              <MessageSquareText className="h-5 w-5" />
            </span>
            <span className="tabular-nums text-[12px] font-semibold text-white drop-shadow">
              {formatCompactCount(video.comments.length)}
            </span>
          </button>

          {/* Bookmark / Save */}
          <button
            type="button"
            onClick={() => onToggleBookmark(video.id)}
            aria-label={video.bookmarkedByMe ? 'Hapus dari simpanan' : 'Simpan video'}
            className="group flex flex-col items-center gap-1 focus-visible:outline-none"
          >
            <span
              className={`flex h-12 w-12 items-center justify-center rounded-full backdrop-blur-md transition duration-150 active:scale-90 ${
                video.bookmarkedByMe
                  ? 'bg-white text-zinc-950'
                  : 'bg-zinc-900/65 text-white hover:bg-zinc-800/80'
              }`}
            >
              <Bookmark
                className={`h-5 w-5 ${video.bookmarkedByMe ? 'fill-zinc-950' : ''}`}
              />
            </span>
            <span className="text-[11px] font-medium text-white drop-shadow">
              {video.bookmarkedByMe ? 'Disimpan' : 'Simpan'}
            </span>
          </button>

          {/* Effects Studio */}
          <button
            type="button"
            onClick={() => onOpenEffects(video.id)}
            aria-label="Edit efek video"
            className="group flex flex-col items-center gap-1 focus-visible:outline-none"
          >
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-900/65 text-white backdrop-blur-md transition duration-150 hover:bg-zinc-800/80 active:scale-90">
              <SlidersHorizontal className="h-5 w-5" />
            </span>
            <span className="text-[11px] font-medium text-white drop-shadow">
              Efek
            </span>
          </button>

          {/* Spinning Creator Audio Disc */}
          <button
            type="button"
            onClick={onOpenProfile}
            aria-label="Buka profil kreator"
            title="Ubah foto & profil"
            className="mt-1 flex h-11 w-11 items-center justify-center rounded-full border-2 border-zinc-700/90 bg-zinc-900 p-0.5 shadow-lg transition hover:border-white"
          >
            <img
              src={video.creatorAvatar}
              alt={video.creatorName}
              referrerPolicy="no-referrer"
              className={`h-full w-full rounded-full object-cover ${
                isActive && isPlaying ? 'animate-spin-disc' : 'animate-spin-disc-paused'
              }`}
            />
          </button>
        </div>

        {/* Bottom-Left Creator Profile & Caption Overlay */}
        <div className="absolute bottom-5 left-3.5 right-20 z-20 flex flex-col gap-2">
          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={onOpenProfile}
              className="group flex items-center gap-2.5 text-left focus-visible:outline-none"
            >
              <img
                src={video.creatorAvatar}
                alt={video.creatorName}
                referrerPolicy="no-referrer"
                className="h-10 w-10 shrink-0 rounded-full border border-white/30 object-cover transition group-hover:scale-105"
              />
              <div className="min-w-0">
                <p className="truncate text-sm font-bold text-white drop-shadow">
                  {video.creatorHandle}
                </p>
                <p className="truncate text-xs text-zinc-300/90">
                  {video.creatorName}
                </p>
              </div>
            </button>

            <button
              type="button"
              onClick={onOpenProfile}
              className="ml-1 h-8 rounded-lg bg-white/15 px-3 text-xs font-semibold text-white backdrop-blur-md transition hover:bg-white/25 active:scale-95 whitespace-nowrap shrink-0"
            >
              Ganti Foto
            </button>
          </div>

          <button
            type="button"
            data-interactive="true"
            onClick={() => setDescExpanded((prev) => !prev)}
            className="text-left focus-visible:outline-none"
          >
            <h2 className="line-clamp-2 text-[15px] font-semibold leading-snug text-white drop-shadow">
              {video.title}
            </h2>
            {descExpanded && video.description && (
              <p className="mt-1 text-xs leading-relaxed text-zinc-200/95">
                {video.description}
              </p>
            )}
          </button>

          {/* Clean Unboxed Metadata */}
          <div className="flex flex-wrap items-center gap-1.5 text-xs text-zinc-300/90">
            <span>{sourceLabel}</span>
            <span aria-hidden="true">·</span>
            <span className="font-mono-tabular">{formatDuration(duration)}</span>
            {video.effect.preset !== 'normal' && (
              <>
                <span aria-hidden="true">·</span>
                <span>{presetLabel}</span>
              </>
            )}
          </div>
        </div>

        {/* Bottom Edge Seekable Progress Bar */}
        <div
          data-interactive="true"
          role="slider"
          aria-label="Posisi putar video"
          aria-valuemin={0}
          aria-valuemax={Math.round(duration)}
          aria-valuenow={Math.round(currentTime)}
          tabIndex={0}
          onPointerDown={(e) => {
            setIsScrubbing(true);
            e.currentTarget.setPointerCapture(e.pointerId);
            handleScrub(e.clientX, e.currentTarget);
          }}
          onPointerMove={(e) => {
            if (isScrubbing) {
              handleScrub(e.clientX, e.currentTarget);
            }
          }}
          onPointerUp={() => setIsScrubbing(false)}
          onPointerCancel={() => setIsScrubbing(false)}
          className="group/bar absolute inset-x-0 bottom-0 z-30 flex h-5 cursor-pointer items-end"
        >
          {isScrubbing && (
            <div className="pointer-events-none absolute bottom-5 left-4 rounded-md bg-black/80 px-2 py-1 font-mono-tabular text-xs font-medium text-white backdrop-blur-md">
              {formatDuration(currentTime)} / {formatDuration(duration)}
            </div>
          )}
          <div className="relative h-1 w-full bg-white/25 transition-all group-hover/bar:h-1.5">
            <div
              className="h-full bg-[#FF2D55] transition-[width] duration-75"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
