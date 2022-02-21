import React, { useState } from "react";
import { Moment } from "moment";
import { ComponentStory } from "@storybook/react";
import DateWidget from "./DateWidget";

export default {
  title: "Core/DateWidget",
  component: DateWidget,
};

const Template: ComponentStory<typeof DateWidget> = args => {
  const [date, setDate] = useState<Moment>();
  return <DateWidget {...args} date={date} onChangeDate={setDate} />;
};

export const Default = Template.bind({});

export const WithTime = Template.bind({});
WithTime.args = {
  hasTime: true,
};
