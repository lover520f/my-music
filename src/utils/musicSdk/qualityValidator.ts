import RNFetchBlob from 'rn-fetch-blob';
import { getFileExtension } from '@/screens/Home/Views/Mylist/MusicList/download/utils';

/**
 * 音质验证器 - 用于验证下载的音乐文件是否符合预期的音质
 */
export interface QualityInfo {
  type: LX.Quality;
  size: number; // 文件大小（字节）
  extension: string;
}

export interface ValidationResult {
  isValid: boolean;
  actualQuality: QualityInfo | null;
  expectedQuality: QualityInfo | null;
  warning?: string;
}

/**
 * 根据文件大小估算音质
 * 这是一个启发式方法，用于检测实际下载的音质是否符合预期
 */
export const estimateQualityBySize = (
  fileSize: number,
  duration: number, // 歌曲时长（秒）
  extension: string
): { quality: LX.Quality; confidence: 'high' | 'medium' | 'low' } => {
  if (!duration || duration <= 0) {
    return { quality: 'flac', confidence: 'low' };
  }

  const bitrate = (fileSize * 8) / (duration * 1000); // kbps

  // 根据扩展名和比特率综合判断
  const ext = extension.toLowerCase();

  // hires (24bit/96kHz+)
  if (ext.includes('flac') && bitrate > 2000) {
    return { quality: 'hires', confidence: 'high' };
  }

  // flac (16bit/44.1kHz)
  if (ext.includes('flac')) {
    if (bitrate > 800 && bitrate <= 2000) {
      return { quality: 'flac', confidence: 'high' };
    }
    if (bitrate > 400 && bitrate <= 800) {
      return { quality: 'flac', confidence: 'medium' };
    }
    return { quality: 'flac', confidence: 'low' };
  }

  // 320k
  if (bitrate >= 280 && bitrate <= 360) {
    return { quality: '320k', confidence: 'high' };
  }

  // 192k
  if (bitrate >= 160 && bitrate < 280) {
    return { quality: '192k', confidence: 'medium' };
  }

  // 128k
  if (bitrate >= 96 && bitrate < 160) {
    return { quality: '128k', confidence: 'high' };
  }

  // 默认根据比特率判断
  if (bitrate >= 320) return { quality: '320k', confidence: 'medium' };
  if (bitrate >= 192) return { quality: '192k', confidence: 'medium' };
  if (bitrate >= 128) return { quality: '128k', confidence: 'medium' };

  return { quality: '128k', confidence: 'low' };
};

/**
 * 验证下载的音乐文件
 */
export const validateDownloadedFile = async (
  filePath: string,
  expectedQuality: LX.Quality,
  expectedExtension: string,
  songDuration: number
): Promise<ValidationResult> => {
  try {
    const stat = await RNFetchBlob.fs.stat(filePath);
    const fileSize = stat.size;
    const extension = getFileExtension(filePath) || expectedExtension;

    const { quality: actualQuality, confidence } = estimateQualityBySize(
      fileSize,
      songDuration,
      extension
    );

    // 检查扩展名是否匹配
    const extensionMatch = extension.toLowerCase().includes(expectedExtension.toLowerCase());

    // 检查音质是否匹配
    const qualityMatch = actualQuality === expectedQuality ||
      (expectedQuality === 'hires' && actualQuality === 'flac') ||
      (expectedQuality === 'flac' && actualQuality === '320k');

    const isValid = extensionMatch && (qualityMatch || confidence === 'high');

    if (!isValid) {
      console.warn(
        `[Quality Validator] Quality mismatch detected!\n` +
        `  Expected: ${expectedQuality} (.${expectedExtension})\n` +
        `  Actual: ${actualQuality} (.${extension})\n` +
        `  File size: ${(fileSize / 1024 / 1024).toFixed(2)} MB\n` +
        `  Bitrate: ${((fileSize * 8) / (songDuration * 1000)).toFixed(0)} kbps\n` +
        `  Confidence: ${confidence}`
      );
    }

    return {
      isValid,
      actualQuality: {
        type: actualQuality,
        size: fileSize,
        extension,
      },
      expectedQuality: {
        type: expectedQuality,
        size: 0, // 预期大小未知
        extension: expectedExtension,
      },
      warning: !isValid
        ? `实际下载的音质(${actualQuality})与预期(${expectedQuality})不匹配`
        : undefined,
    };
  } catch (error) {
    console.error('[Quality Validator] Failed to validate file:', error);
    return {
      isValid: false,
      actualQuality: null,
      expectedQuality: {
        type: expectedQuality,
        size: 0,
        extension: expectedExtension,
      },
      warning: '无法验证文件音质',
    };
  }
};

/**
 * 获取预期的文件大小（用于对比）
 */
export const estimateExpectedFileSize = (
  duration: number, // 秒
  quality: LX.Quality
): { minSize: number; maxSize: number } => {
  const durationSeconds = duration;

  // 根据音质类型估算文件大小范围
  switch (quality) {
    case 'master':
    case 'hires':
      // 24bit/96kHz+ 无损，通常 > 2000 kbps
      return {
        minSize: durationSeconds * 2000 * 1000 / 8,  // 最小约25MB/分钟
        maxSize: durationSeconds * 4000 * 1000 / 8,  // 最大约50MB/分钟
      };
    case 'flac':
      // 16bit/44.1kHz 无损，通常 800-2000 kbps
      return {
        minSize: durationSeconds * 800 * 1000 / 8,
        maxSize: durationSeconds * 2000 * 1000 / 8,
      };
    case '320k':
      return {
        minSize: durationSeconds * 280 * 1000 / 8,
        maxSize: durationSeconds * 360 * 1000 / 8,
      };
    case '192k':
      return {
        minSize: durationSeconds * 160 * 1000 / 8,
        maxSize: durationSeconds * 220 * 1000 / 8,
      };
    case '128k':
    default:
      return {
        minSize: durationSeconds * 96 * 1000 / 8,
        maxSize: durationSeconds * 160 * 1000 / 8,
      };
  }
};
