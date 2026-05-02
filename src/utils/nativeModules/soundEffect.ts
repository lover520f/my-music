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

export async function setAudioSessionId(audioSessionId: number) {
  try {
    if (NativeModules.LXSoundEffect) {
      await NativeModules.LXSoundEffect.setAudioSessionId(audioSessionId)
    }
  } catch (error) {
    console.error('Failed to set audio session ID:', error)
  }
}

export async function isSoundEffectModuleSupported(): Promise<boolean> {
  try {
    if (NativeModules.LXSoundEffect) {
      return await NativeModules.LXSoundEffect.isSupported()
    }
    return false
  } catch (error) {
    console.error('Failed to check sound effect support:', error)
    return false
  }
}
