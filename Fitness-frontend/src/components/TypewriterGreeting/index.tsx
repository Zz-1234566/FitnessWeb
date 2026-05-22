import React, { memo, useEffect, useState } from 'react';
import './index.less';

export type TypewriterGreetingMessage = {
  title: string;
  sub: string;
};

type TypewriterGreetingProps = {
  messages: TypewriterGreetingMessage[];
  className?: string;
  titleClassName: string;
  subClassName: string;
};

const TypewriterGreeting: React.FC<TypewriterGreetingProps> = ({
  messages,
  className,
  titleClassName,
  subClassName,
}) => {
  const [typedTitle, setTypedTitle] = useState('');
  const [typedSub, setTypedSub] = useState('');
  const [messageIndex, setMessageIndex] = useState(0);
  const [phase, setPhase] = useState<
    'typingTitle' | 'typingSub' | 'pausing' | 'deletingSub' | 'deletingTitle' | 'pausedAfterDelete'
  >('typingTitle');

  useEffect(() => {
    setTypedTitle('');
    setTypedSub('');
    setMessageIndex(0);
    setPhase('typingTitle');
  }, [messages]);

  useEffect(() => {
    if (!messages.length) {
      return undefined;
    }

    const currentMessage = messages[messageIndex] || messages[0];
    let delay = 118;

    if (phase === 'typingSub') {
      delay = 76;
    } else if (phase === 'pausing' || phase === 'pausedAfterDelete') {
      delay = 3000;
    } else if (phase === 'deletingSub') {
      delay = 78;
    } else if (phase === 'deletingTitle') {
      delay = 90;
    }

    const timer = window.setTimeout(() => {
      switch (phase) {
        case 'typingTitle':
          if (typedTitle.length < currentMessage.title.length) {
            setTypedTitle(currentMessage.title.slice(0, typedTitle.length + 1));
          } else {
            setPhase('typingSub');
          }
          return;
        case 'typingSub':
          if (typedSub.length < currentMessage.sub.length) {
            setTypedSub(currentMessage.sub.slice(0, typedSub.length + 1));
          } else {
            setPhase('pausing');
          }
          return;
        case 'pausing':
          setPhase('deletingSub');
          return;
        case 'deletingSub':
          if (typedSub.length > 0) {
            setTypedSub((prev) => prev.slice(0, -1));
          } else {
            setPhase('deletingTitle');
          }
          return;
        case 'deletingTitle':
          if (typedTitle.length > 0) {
            setTypedTitle((prev) => prev.slice(0, -1));
          } else {
            setPhase('pausedAfterDelete');
          }
          return;
        case 'pausedAfterDelete':
          setMessageIndex((prev) => (prev + 1) % messages.length);
          setPhase('typingTitle');
          return;
        default:
      }
    }, delay);

    return () => window.clearTimeout(timer);
  }, [messageIndex, messages, phase, typedSub, typedTitle]);

  const showTitleCaret =
    phase === 'typingTitle' || phase === 'deletingTitle' || phase === 'pausedAfterDelete';
  const showSubCaret = phase === 'typingSub' || phase === 'deletingSub' || phase === 'pausing';

  if (!messages.length) {
    return null;
  }

  return (
    <div className={className}>
      <div className="typewriter-greeting__title-wrap">
        <h1 className={`${titleClassName} typewriter-greeting__title`}>
          <span>{typedTitle}</span>
          {showTitleCaret && <span className="typewriter-greeting__caret" aria-hidden="true" />}
        </h1>
      </div>
      <div className="typewriter-greeting__sub-wrap">
        <p className={`${subClassName} typewriter-greeting__sub`}>
          <span>{typedSub}</span>
          {showSubCaret && (
            <span
              className="typewriter-greeting__caret typewriter-greeting__caret--sub"
              aria-hidden="true"
            />
          )}
        </p>
      </div>
    </div>
  );
};

export const buildGreetingMessages = (
  titleVariants: string[],
  subVariants: string[],
): TypewriterGreetingMessage[] =>
  subVariants.map((sub, index) => ({
    title: titleVariants[index % Math.max(titleVariants.length, 1)] || '',
    sub,
  }));

export default memo(TypewriterGreeting);
