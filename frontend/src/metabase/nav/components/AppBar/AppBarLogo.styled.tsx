import styled from "@emotion/styled";
import { css } from "@emotion/react";
import { color } from "metabase/lib/colors";
import Link from "metabase/core/components/Link";
import { space } from "metabase/styled-components/theme";

export const LogoRoot = styled.div`
  position: relative;
`;

export const LogoLink = styled(Link)`
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.375rem;
  padding: 0.5rem 1rem;
  transition: opacity 0.3s;

  &:hover {
    background-color: ${color("bg-light")};
  }
`;

interface ToggleContainerProps {
  isAbsolute?: boolean;
}

export const ToggleContainer = styled.div<ToggleContainerProps>`
  ${props =>
    props.isAbsolute
      ? css`
          position: absolute;
          top: 0.625rem;
          left: 0.9375rem;
        `
      : css`
          padding: ${space(1)} ${space(2)};
        `}
  transition: opacity 0.3s;
`;
