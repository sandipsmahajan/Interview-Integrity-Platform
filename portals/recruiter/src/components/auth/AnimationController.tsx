import { createContext, useContext, type ReactNode } from 'react';
import { useReducedMotion } from 'framer-motion';

interface AnimationState {
  enabled: boolean;
}

const AnimationContext = createContext<AnimationState>({ enabled: true });

/**
 * Central controller for decorative motion in the auth showcase. Disables
 * looping animations when the user prefers reduced motion so every consumer
 * (bubbles, carousel, backgrounds) honors it from one place.
 */
export function AnimationController({ children }: { children: ReactNode }) {
  const reduceMotion = useReducedMotion();
  return <AnimationContext.Provider value={{ enabled: !reduceMotion }}>{children}</AnimationContext.Provider>;
}

export function useAnimationController() {
  return useContext(AnimationContext);
}
