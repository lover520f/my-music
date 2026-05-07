import { NativeModules, Platform } from 'react-native'

export const isSoundEffectSupported = Platform.OS === 'ios' || Platform.OS === 'android'

export async function updateNativeSoundEffectConfig(config: any) {
  try {
    if (NativeModules.LXSoundEffect) {
      await NativeModules.LXSoundEffect.updateConfig(config)
    }
  } catch (error) {
    console.error('Failed to update sound effect config:', error)
  }
}

export async function setAudioSessionId(sessionId: number) {
  try {
    if (NativeModules.LXSoundEffect) {
      await NativeModules.LXSoundEffect.setAudioSessionId(sessionId)
    }
  } catch (error) {
    console.error('Failed to set audio session ID:', error)
  }
}

export async function getCurrentAudioSessionId(): Promise<number> {
  try {
    if (NativeModules.LXSoundEffect) {
      return await NativeModules.LXSoundEffect.getCurrentAudioSessionId()
    }
    return 0
  } catch (error) {
    console.error('Failed to get audio session ID:', error)
    return 0
  }
}
