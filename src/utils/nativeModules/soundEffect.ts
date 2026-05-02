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
