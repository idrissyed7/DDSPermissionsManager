FROM node

ARG VITE_BACKEND_URL

WORKDIR /usr/src/app

COPY . /usr/src/app

RUN npm install

RUN npm run build

EXPOSE 3000

CMD [ "node", "build" ]
